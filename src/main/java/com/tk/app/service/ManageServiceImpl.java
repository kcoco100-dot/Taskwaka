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

	// 필수 번호값 확인
	private Long requiredLong(Map<String, Object> map, String key) {
		if (map == null || !(map.get(key) instanceof Number)) {
			throw new IllegalArgumentException(key + " 값이 올바르지 않습니다.");
		}

		return ((Number) map.get(key)).longValue();
	}

	// 워크스페이스 번호 조회
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

	// 팀원 배정용 프로젝트 조회
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

	// 캘린더 프로젝트 및 업무 조회
	@Override
	public Map<String, Object> getManageCalendarData(Map<String, Object> map) throws Exception {
		Map<String, Object> result = new HashMap<>();
		List<ManageCalendarDto> projectList = new ArrayList<>();
		List<ManageCalendarDto> taskList = new ArrayList<>();
		try {
			requiredLong(map, "workspaceId");
			requiredLong(map, "memberId");
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

	// 본인 워크 참여번호 조회
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

    // 프로젝트 배정 및 직위 변경
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignOrUpdateProjectMember(Map<String, Object> map) throws Exception {
        try {
            Long workspaceId = mapper.findByDomainKey((String) map.get("domainKey"));
            Long workspaceMemberId = requiredLong(map, "workspaceMemberId");
            Long projectId = requiredLong(map, "projectId");
            int role = requiredLong(map, "role").intValue();
            if (role != 10 && role != 30 && role != 50 && role != 70) {
                throw new IllegalArgumentException("직위 값이 올바르지 않습니다.");
            }
            if (projectMemberRepository.countValidAssignmentTarget(workspaceId, workspaceMemberId, projectId) != 1) {
                throw new IllegalArgumentException("프로젝트 또는 팀원을 확인하세요.");
            }
            Long projectMemberId = projectMemberRepository.findAssignedProjectMemberId(projectId, workspaceMemberId);
            if (projectMemberId != null && role < 30
                    && projectMemberRepository.countAssignedTasks(workspaceId, projectMemberId) > 0) {
                throw new IllegalStateException("업무 담당자는 TL 미만으로 변경할 수 없습니다.");
            }
            projectMemberRepository.upsertProjectMember(workspaceMemberId, projectId, role);
        } catch (Exception e) {
            log.info("assignOrUpdateProjectMember : ", e);
            throw e;
        }
    }
    // 프로젝트 팀원 제외
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excludeProjectMember(Map<String, Object> map) throws Exception {
        try {
            Long workspaceId = mapper.findByDomainKey((String) map.get("domainKey"));
            Long projectMemberId = requiredLong(map, "projectMemberId");
            if (projectMemberRepository.countActiveProjectMember(workspaceId, projectMemberId) != 1) {
                throw new IllegalArgumentException("프로젝트 참여자를 확인하세요.");
            }
            if (projectMemberRepository.countAssignedTasks(workspaceId, projectMemberId) > 0) {
                throw new IllegalStateException("업무 담당자는 프로젝트에서 제외할 수 없습니다.");
            }
            projectMemberRepository.excludeFromProject(projectMemberId);
        } catch (Exception e) {
            log.info("excludeProjectMember : ", e);
            throw e;
        }
    }
    // 워크 팀원 제외
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excludeWorkspaceMember(Map<String, Object> map) throws Exception {
        try {
            Long workspaceId = mapper.findByDomainKey((String) map.get("domainKey"));
            Long workspaceMemberId = requiredLong(map, "workspaceMemberId");
            if (projectMemberRepository.countActiveWorkspaceMember(workspaceId, workspaceMemberId) != 1) {
                throw new IllegalArgumentException("워크 참여자를 확인하세요.");
            }
            if (projectMemberRepository.countWorkspaceAssignedTasks(workspaceId, workspaceMemberId) > 0) {
                throw new IllegalStateException("업무 담당자는 워크에서 제외할 수 없습니다.");
            }
            List<Long> projectMemberIds = projectMemberRepository.findActiveProjectMemberIds(workspaceId, workspaceMemberId);
            for (Long projectMemberId : projectMemberIds) {
                projectMemberRepository.excludeFromProject(projectMemberId);
            }
            projectMemberRepository.excludeFromWorkspace(workspaceMemberId);
        } catch (Exception e) {
            log.info("excludeWorkspaceMember : ", e);
            throw e;
        }
    }
}
