package com.tk.app.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.domain.dto.ManageCalendarDto;
import com.tk.app.mapper.ManageMapper;
import com.tk.app.repository.ProjectMemberRepository;
import com.tk.app.repository.ProjectTeamView;
import com.tk.app.security.LoginMemberUtil;

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

	// 전달받은 배정 목록 정리
	private Map<Long, Integer> teamAssignmentMap(Object source, Set<Long> available) {
	    Map<Long, Integer> result = new LinkedHashMap<>();
	    if (!(source instanceof List<?> members) || members.size() > available.size()) {
	        throw new IllegalArgumentException("배정 명단을 확인하세요.");
	    }
	    for (Object item : members) {
	        if (!(item instanceof Map<?, ?> member)) {
	            throw new IllegalArgumentException("배정 정보 형식이 올바르지 않습니다.");
	        }
	        Map<String, Object> memberMap = new HashMap<>();
	        memberMap.put("workspaceMemberId", member.get("workspaceMemberId"));
	        memberMap.put("role", member.get("role"));
	        Long id = requiredLong(memberMap, "workspaceMemberId");
	        Long role = requiredLong(memberMap, "role");
	        if (!available.contains(id) || !Set.of(70L, 50L, 30L, 10L).contains(role)) {
	            throw new IllegalArgumentException("현재 워크의 활성 멤버와 올바른 직위만 배정할 수 있습니다.");
	        }
	        if (result.putIfAbsent(id, role.intValue()) != null) {
	            throw new IllegalArgumentException("한 사람을 중복 배정할 수 없습니다.");
	        }
	    }
	    return result;
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
			SessionInfo info = LoginMemberUtil.getSessionInfo();
	        if (map == null) {
	            throw new IllegalArgumentException("프로젝트 조회 조건이 없습니다.");
	        }
			
			Map<String, Object> queryMap = new HashMap<>(map);
	        queryMap.put("memberId", info.getMember_id());
			
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
    
    // 프로젝트 팀원 관리 조회
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectMemberForm(String domainKey, Long actorId, Long projectId) throws Exception {
        Map<String, Object> map = new HashMap<>();
        try {
            if (actorId == null) {
                throw new SecurityException("로그인이 필요합니다.");
            }
            Long workspaceId = projectMemberRepository.findWorkspaceIdForTeam(domainKey);
            if (workspaceId == null) {
                throw new IllegalArgumentException("워크스페이스가 없습니다.");
            }
            List<ManageDto> projectList = new ArrayList<>();
            for (Object[] row : projectMemberRepository.findProjectsForTeamManagement(workspaceId, actorId)) {
                ManageDto project = new ManageDto();
                project.setProject_id(((Number) row[0]).longValue());
                project.setWorkspace_id(workspaceId);
                project.setSubject(row[1] == null ? "" : row[1].toString());
                projectList.add(project);
            }
            if (projectId == null && !projectList.isEmpty()) {
                projectId = projectList.get(0).getProject_id();
            }
            ManageDto selectedProject = null;
            for (ManageDto project : projectList) {
                if (project.getProject_id().equals(projectId)) {
                    selectedProject = project;
                    break;
                }
            }
            if (projectId != null && selectedProject == null) {
                throw new SecurityException("관리할 수 없는 프로젝트입니다.");
            }
            Map<Long, Map<String, Object>> members = new LinkedHashMap<>();
            if (selectedProject != null) {
                for (ProjectTeamView row : projectMemberRepository.findWorkspaceMembersWithProject(workspaceId, "")) {
                    Long id = row.getWorkspaceMemberId();
                    Map<String, Object> member = members.get(id);
                    if (member == null) {
                        member = new LinkedHashMap<>();
                        member.put("id", id.toString());
                        member.put("name", row.getName());
                        member.put("role", null);
                        members.put(id, member);
                    }
                    if (projectId.equals(row.getProjectId())) {
                        if (member.get("role") != null || row.getRole() == null) {
                            throw new IllegalStateException("기존 프로젝트 참여 정보를 확인하세요.");
                        }
                        member.put("role", row.getRole());
                    }
                }
            }
            map.put("projectList", projectList);
            map.put("selectedProject", selectedProject);
            map.put("selectedProjectId", projectId);
            map.put("teamMembers", new ArrayList<>(members.values()));
        } catch (Exception e) {
            log.info("getProjectMemberForm : ", e);
            
            throw e;
        }
        return map;
    }

    // 프로젝트 팀원 일괄 저장
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveProjectMembers(String domainKey, Long actorId, Map<String, Object> map) throws Exception {
        int result = 0;
        try {
            if (actorId == null || map == null) {
                throw new IllegalArgumentException("로그인 정보와 저장할 정보를 확인하세요.");
            }
            Long projectId = requiredLong(map, "projectId");
            Long workspaceId = projectMemberRepository.findWorkspaceIdForTeam(domainKey);
            
            Set<Long> available = new LinkedHashSet<>();
            Map<Long, Integer> current = new LinkedHashMap<>();
            Map<Long, Long> projectMemberIds = new HashMap<>();
            
            for (ProjectTeamView row : projectMemberRepository.findWorkspaceMembersWithProject(workspaceId, "")) {
                Long id = row.getWorkspaceMemberId();
                available.add(id);
                if (projectId.equals(row.getProjectId())) {
                    if (row.getRole() == null || current.putIfAbsent(id, row.getRole()) != null) {
                        throw new IllegalStateException("기존 프로젝트 참여 정보를 확인하세요.");
                    }
                    projectMemberIds.put(id, row.getProjectMemberId());
                }
            }
            Map<Long, Integer> original =
                    teamAssignmentMap(map.get("originalMembers"), available);
            Map<Long, Integer> desired =
                    teamAssignmentMap(map.get("members"), available);
            if (!current.equals(original)) {
                throw new IllegalStateException("팀원 구성이 변경되었습니다. 새로고침 후 다시 확인하세요.");
            }
            Set<Long> ids = new LinkedHashSet<>(current.keySet());
            ids.addAll(desired.keySet());
            
            for (Long id : ids) {
                Integer before = current.get(id);
                Integer after = desired.get(id);
                if (Objects.equals(before, after)) {
                    continue;
                }
                
                Map<String, Object> changeMap = new HashMap<>();
                changeMap.put("domainKey", domainKey);
                changeMap.put("actorId", actorId);
                if (after == null) {
                    Long projectMemberId = projectMemberIds.get(id);
                    if (projectMemberId == null) {
                        throw new IllegalStateException("프로젝트 참여 정보를 찾을 수 없습니다.");
                    }
                    changeMap.put("projectMemberId", projectMemberId);
                    excludeProjectMember(changeMap);
                } else {
                    changeMap.put("workspaceMemberId", id);
                    changeMap.put("projectId", projectId);
                    changeMap.put("role", after);
                    assignOrUpdateProjectMember(changeMap);
                }
                
                result++;
            }
            
        } catch (Exception e) {
            log.info("saveProjectMembers : ", e);
            throw e;
        }
        return result;
    }

}
