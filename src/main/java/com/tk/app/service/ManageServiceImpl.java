package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.mapper.ManageMapper;
import com.tk.app.repository.ProjectMemberRepository;
import com.tk.app.repository.ProjectTeamView;

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

        return workspaceId;
    }

    // TL 업무 참여 관계 정리
    private void clearTaskAssignments(Long projectMemberId) {
        projectMemberRepository.deleteUnlinkedTaskMembers(projectMemberId);
    }

    
	@Override
	public Long findByDomainKey(String domainKey) throws Exception {
		try {
            return mapper.findByDomainKey(domainKey);
        } catch (Exception e) {
            log.info("findByDomainKey : ", e);
            throw e;
        }
	}
	
	@Override
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException {
		List<ManageDto> list = null;
		
		try {
			list = mapper.listProject(map);
			
		} catch (Exception e) {
			log.info("listProject : ", e);
			
			throw e;
		}
		
		return list;
	}
	

	@Override
	public List<ManageDto> listProjectManage(Map<String, Object> map) throws SQLException {
		List<ManageDto> list = null;
		
		try {
			list = mapper.listProjectManage(map);
		} catch (Exception e) {
			log.info("listProject : ", e);
			
			throw e;
		}
		
		return list;
	}
	

	@Override
	public int dataCount(Map<String, Object> map) throws Exception {
		try {
			return mapper.dataCount(map);
        } catch (Exception e) {
        	log.info("dataCount : ", e);

        	throw e;
        }
	}

	@Override
	public List<ProjectTeamView> listWorkspaceMembers(Long workspaceId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getProjectCount(Long workspaceId) throws Exception {
		
		return mapper.getProjectCount(workspaceId);
	}

	@Override
	public List<ManageDto> getProjectDetail(Map<String, Object> map) throws Exception {
		List<ManageDto> list = null;
		try {
			list = mapper.getProjectDetail(map);
		} catch (Exception e) {
			log.info("getProjectDetail : ", e);
		}
		
		return list;
	}

	@Override
	public List<ManageDto> findById(Map<String, Object> map) {
		List<ManageDto> list = null;
		try {
			list = mapper.findById(map);
		} catch (Exception e) {
			log.info("findById : ", e);
		}
		
		return list;
	}

	@Override
	public List<ManageDto> findPrevPost(Map<String, Object> map) {
		List<ManageDto> list = null;
		try {
			list = mapper.findPrevPost(map);
		} catch (Exception e) {
			log.info("findPrevPost : ", e);
		}
		return list;
	}

	@Override
	public List<ManageDto> findNextPost(Map<String, Object> map) {
		List<ManageDto> list = null;
		try {
			list = mapper.findNextPost(map);
		} catch (Exception e) {
			log.info("findNextPost : ", e);
		}
		return list;
	}
	// 프로젝트 팀원 등록 및 직위 변경
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignOrUpdateProjectMember(Map<String, Object> map) throws Exception {

        try {
            Long workspaceId = requireManagePermission(map);

            Long actorId = requiredLong(map, "actorId");
            Long workspaceMemberId = requiredLong(map, "workspaceMemberId");
            Long projectId = requiredLong(map, "projectId");
            int role = requiredLong(map, "role").intValue();

            if (role != 10 && role != 30 && role != 50 && role != 70) {
                throw new IllegalArgumentException("올바르지 않은 직위입니다.");
            }

            Integer actorRole = projectMemberRepository
                    .findRoleByWorkspaceIdAndMemberId(workspaceId, actorId);

            if (actorRole == 50 && role == 70) {
                throw new SecurityException("PM 직위는 배정할 수 없습니다.");
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

            Long actorId = requiredLong(map, "actorId");
            Long projectMemberId = requiredLong(map, "projectMemberId");

            int count = projectMemberRepository.countActiveProjectMember(
                    workspaceId, projectMemberId
            );

            if (count != 1) {
                throw new IllegalArgumentException("프로젝트 참여자를 찾을 수 없습니다.");
            }

            Integer actorRole = projectMemberRepository
                    .findRoleByWorkspaceIdAndMemberId(workspaceId, actorId);

            Integer targetRole = projectMemberRepository
                    .findAssignedProjectRole(projectMemberId);

            if (actorRole == 50 && targetRole != null && targetRole == 70) {
                throw new SecurityException("PM을 제외할 수 없습니다.");
            }

            if (targetRole != null && targetRole == 30) {
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

            List<Long> projectMemberIds = projectMemberRepository
                    .findActiveProjectMemberIds(workspaceId, workspaceMemberId);

            for (Long projectMemberId : projectMemberIds) {
                Integer targetRole = projectMemberRepository
                        .findAssignedProjectRole(projectMemberId);

                if (actorRole == 50 && targetRole != null && targetRole == 70) {
                    throw new SecurityException("PM은 워크스페이스에서 제외할 수 없습니다.");
                }

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
