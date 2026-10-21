package com.tk.app.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageCalendarDto;
import com.tk.app.mapper.ManageMapper;
import com.tk.app.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageServiceImpl implements ManageService{
	private final ManageMapper mapper;
	private final ProjectMemberRepository projectMemberRepository;

	// Map 필수 번호값 확인
	private Long requiredLong(Map<String, Object> map, String key) {
		if (map == null || !(map.get(key) instanceof Number)) {
			throw new IllegalArgumentException(key + " 값이 올바르지 않습니다.");
		}

		return ((Number) map.get(key)).longValue();
	}

	// 워크스페이스 팀원 관리 권한 확인
	private Long requireManagePermission(Map<String, Object> map)
			throws Exception {

		if (map == null
				|| !(map.get("domainKey") instanceof String)
				|| ((String) map.get("domainKey")).isBlank()) {
			throw new IllegalArgumentException("워크스페이스 정보가 없습니다.");
		}

		String domainKey = (String) map.get("domainKey");
		Long actorId = requiredLong(map, "actorId");

		Long workspaceId = mapper.findByDomainKey(domainKey);

		if (workspaceId == null) {
			throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
		}

		Integer actorRole = projectMemberRepository
				.findRoleByWorkspaceIdAndMemberId(workspaceId, actorId);

		if (actorRole == null || actorRole < 50) {
			throw new SecurityException("팀원 관리 권한이 없습니다.");
		}

		map.put("workspaceId", workspaceId); // 프로젝트/워크스페이스 제외 SQL에 전달
		return workspaceId;
	}

	// 프로젝트별 관리 권한: 워크 PM 또는 해당 프로젝트 DT
	private int requireProjectPermission(Map<String, Object> map, Long workspaceId) {
		map.put("workspaceId", workspaceId);
		Integer role = mapper.getManageRoleForProject(map);
		if (role == null || role < 50) {
			throw new SecurityException("해당 프로젝트 관리 권한이 없습니다.");
		}
		return role;
	}

	// TL 업무 참여 관계 정리
	private void clearTaskAssignments(Long projectMemberId) {
		projectMemberRepository.deleteUnlinkedTaskMembers(projectMemberId);
	}

	@Override
	public Long findByDomainKey(String domainKey) throws Exception {
		Long result = null;
		try {
			result = mapper.findByDomainKey(domainKey);
		} catch (Exception e) {
			log.info("findByDomainKey : ", e);
			throw e;
		}
		return result;
	}

	@Override
	public List<ManageDto> listProjectManage(Map<String, Object> map) throws SQLException {
		List<ManageDto> list = null;

		try {
			list = mapper.listProjectManage(map);
		} catch (Exception e) {
			log.info("listProjectManage : ", e);

			throw e;
		}

		return list;
	}

	// 캘린더 프로젝트 및 업무 정보 조회
	@Override
	public Map<String, Object> getManageCalendarData(Map<String, Object> map) throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<ManageCalendarDto> projectList = new ArrayList<>();
		List<ManageCalendarDto> taskList = new ArrayList<>();
		try {
			requiredLong(map, "workspaceId");
			requiredLong(map, "memberId");
			Integer role = mapper.findManageCalendarRole(map);
			if (role == null || role < 10) {
				throw new SecurityException("워크스페이스 참여자만 캘린더를 조회할 수 있습니다.");
			}
			projectList = mapper.listManageCalendarProjects(map);
			taskList = mapper.listManageCalendarTasks(map);
			result.put("myProjects", projectList);
			result.put("myTasks", taskList);
		} catch (Exception e) {
			log.info("getManageCalendarData : ", e);
			throw e;
		}
		return result;
	}

	@Override
	public Long getOwnWorkspaceMemberId(Map<String, Object> map) {
		Long result = null;
		try {
			result = mapper.getOwnWorkspaceMemberId(map);
		} catch (Exception e) {
			log.info("getOwnWorkspaceMemberId : ", e);
			throw e;
		}
		return result;
	}

	// 프로젝트 팀원 등록 및 직위 변경
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void assignOrUpdateProjectMember(Map<String, Object> map) throws Exception {

		try {
			Long workspaceId = requireManagePermission(map);

			Long workspaceMemberId = requiredLong(map, "workspaceMemberId");
			Long projectId = requiredLong(map, "projectId");
			int role = requiredLong(map, "role").intValue();

			int actorRole = requireProjectPermission(map, workspaceId);
			Long ownWorkspaceMemberId = mapper.getOwnWorkspaceMemberId(map);
			boolean ownPm = actorRole == 70 && role == 70 && workspaceMemberId.equals(ownWorkspaceMemberId);
			if (role != 10 && role != 30 && role != 50 && !ownPm) {
				throw new IllegalArgumentException("PM 직위는 본인에게만 배정할 수 있습니다.");
			}
			if (actorRole == 50 && role != 30) {
				throw new SecurityException("DT는 본인 프로젝트에서 TL만 지정할 수 있습니다.");
			}
			Integer targetWorkspaceRole = mapper.getTargetWorkspaceRole(map);
			if (targetWorkspaceRole != null && targetWorkspaceRole == 70 && !ownPm) {
				throw new SecurityException("워크스페이스 PM은 본인 PM 직위만 배정할 수 있습니다.");
			}

			int count = projectMemberRepository.countValidAssignmentTarget(
					workspaceId, workspaceMemberId, projectId
			);

			if (count != 1) {
				throw new IllegalArgumentException("해당 워크스페이스의 프로젝트 또는 팀원이 아닙니다.");
			}

			Long projectMemberId = projectMemberRepository
					.findAssignedProjectMemberId(projectId, workspaceMemberId);

			if (projectMemberId != null) {
				Integer oldRole = projectMemberRepository
						.findAssignedProjectRole(projectMemberId);

				if (actorRole == 50 && oldRole != null && oldRole != 30) {
					throw new SecurityException("DT는 TL 직위만 변경할 수 있습니다.");
				}
				if (oldRole != null && oldRole == 70 && !ownPm) {
					throw new SecurityException("다른 PM의 직위는 변경할 수 없습니다.");
				}
				if (oldRole != null && oldRole == 30 && role != 30) {
					clearTaskAssignments(projectMemberId);
				}
			}

			projectMemberRepository.upsertProjectMember(
					workspaceMemberId, projectId, role
			);

		} catch (Exception e) {
			log.error("assignOrUpdateProjectMember 오류: ", e);
			throw e;
		}
	}

	// 프로젝트에서 팀원 제외
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void excludeProjectMember(Map<String, Object> map) throws Exception {

		try {
			Long workspaceId = requireManagePermission(map);

			Long projectMemberId = requiredLong(map, "projectMemberId");

			int count = projectMemberRepository.countActiveProjectMember(
					workspaceId, projectMemberId
			);

			if (count != 1) {
				throw new IllegalArgumentException("프로젝트 참여자를 찾을 수 없습니다.");
			}

			Long projectId = mapper.getProjectIdForProjectMember(map);
			if (projectId == null) {
				throw new IllegalArgumentException("현재 워크스페이스 프로젝트 참여자가 아닙니다.");
			}
			map.put("projectId", projectId);
			int actorRole = requireProjectPermission(map, workspaceId);
			Integer targetRole = projectMemberRepository.findAssignedProjectRole(projectMemberId);
			if (targetRole == null) {
				throw new IllegalArgumentException("프로젝트 참여자의 직위를 찾을 수 없습니다.");
			}
			if (targetRole == 70) {
				// PM은 자신의 프로젝트 소속만 직접 제외할 수 있음 (워크스페이스 PM 권한은 유지)
				if (actorRole != 70 || mapper.countOwnPmProjectMember(map) != 1) {
					throw new SecurityException("다른 PM을 프로젝트에서 제외할 수 없습니다.");
				}
			} else if (actorRole == 50 && targetRole != 30) {
				throw new SecurityException("DT는 TL만 프로젝트에서 제외할 수 있습니다.");
			}

			if (targetRole == 30) {
				clearTaskAssignments(projectMemberId);
			}

			int result = projectMemberRepository
					.excludeFromProject(projectMemberId);

			if (result != 1) {
				throw new IllegalStateException("프로젝트 제외에 실패했습니다.");
			}

		} catch (Exception e) {
			log.error("excludeProjectMember 오류: ", e);
			throw e;
		}
	}

	// 워크스페이스에서 팀원 제외
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void excludeWorkspaceMember(Map<String, Object> map)
			throws Exception {

		try {
			Long workspaceId = requireManagePermission(map);

			Long actorId = requiredLong(map, "actorId");
			Long workspaceMemberId = requiredLong(map, "workspaceMemberId");

			int count = projectMemberRepository.countActiveWorkspaceMember(
					workspaceId, workspaceMemberId
			);

			if (count != 1) {
				throw new IllegalArgumentException(
						"워크스페이스 참여자를 찾을 수 없습니다."
				);
			}

			Integer actorRole = projectMemberRepository
					.findRoleByWorkspaceIdAndMemberId(workspaceId, actorId);

			if (actorRole != 70) {
				throw new SecurityException("워크스페이스 제외는 PM만 가능합니다.");
			}
			Integer targetWorkspaceRole = mapper.getTargetWorkspaceRole(map);
			if (targetWorkspaceRole != null && targetWorkspaceRole == 70) {
				throw new SecurityException("PM을 워크스페이스에서 제외할 수 없습니다.");
			}
			List<Long> projectMemberIds = projectMemberRepository
					.findActiveProjectMemberIds(workspaceId, workspaceMemberId);

			for (Long projectMemberId : projectMemberIds) {
				Integer targetRole = projectMemberRepository
						.findAssignedProjectRole(projectMemberId);


				if (targetRole != null && targetRole == 30) {
					clearTaskAssignments(projectMemberId);
				}

				int result = projectMemberRepository
						.excludeFromProject(projectMemberId);

				if (result != 1) {
					throw new IllegalStateException("프로젝트 참여자 제외에 실패했습니다.");
				}
			}

			int result = projectMemberRepository
					.excludeFromWorkspace(workspaceMemberId);

			if (result != 1) {
				throw new IllegalStateException("워크스페이스 제외에 실패했습니다.");
			}

		} catch (Exception e) {
			log.error("excludeWorkspaceMember 오류: ", e);
			throw e;
		}
	}


}
