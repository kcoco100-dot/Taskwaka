package com.tk.app.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageTaskDto;
import com.tk.app.mapper.ManageProjectMapper;
import com.tk.app.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageProjectServiceImpl implements ManageProjectService {

    private final ManageProjectMapper mapper;
    private final ProjectMemberRepository projectMemberRepository;

    // 워크스페이스 및 로그인 정보 유효성 검증
    private Long verifiedWorkspaceId(String domainKey, Long memberId) throws Exception {
        try {
            Long workspaceId = mapper.findByDomainKey(domainKey);
            if (workspaceId == null || memberId == null) {
                throw new SecurityException("유효하지 않은 워크스페이스 또는 로그인 정보입니다.");
            }
            Integer role = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspaceId, memberId);
            if (role == null || role < 50) {
                throw new SecurityException("프로젝트 참여자 관리 권한이 없습니다.");
            }
            return workspaceId;
        } catch (Exception e) {
            log.info("verifiedWorkspaceId : ", e);
        }
        return null;
    }

    // 프로젝트 참여자 배정
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignProject(String domainKey, Long memberId, Long workspaceMemberId, Long projectId, int role) throws Exception {
        try {
            Long workspaceId = verifiedWorkspaceId(domainKey, memberId);
            Integer actorRole = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspaceId, memberId);
            if (role != 10 && role != 30 && role != 50 && role != 70) {
                throw new IllegalArgumentException("유효하지 않은 역할입니다.");
            }
            if (actorRole == 50 && role == 70) {
                throw new SecurityException("PM 역할을 부여할 수 없습니다.");
            }
            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("projectId", projectId);
            map.put("workspaceMemberId", workspaceMemberId);
            if (mapper.countProjectInWorkspace(map) != 1 || mapper.countTargetInWorkspace(map) != 1) {
                throw new IllegalArgumentException("프로젝트와 참여자가 같은 워크스페이스에 있지 않습니다.");
            }
            projectMemberRepository.upsertProjectMember(workspaceMemberId, projectId, role);
        } catch (Exception e) {
            log.info("assignProject : ", e);
        }
    }

    // 프로젝트 참여자 제외
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void excludeProject(String domainKey, Long memberId, Long projectMemberId) throws Exception {
        try {
            verifiedWorkspaceId(domainKey, memberId);
            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("projectMemberId", projectMemberId);
            if (mapper.countProjectMemberInWorkspace(map) != 1) {
                throw new SecurityException("해당 워크스페이스의 프로젝트 참여자가 아닙니다.");
            }
            projectMemberRepository.excludeFromProject(projectMemberId);
        } catch (Exception e) {
            log.info("excludeProject : ", e);
        }
    }

    // 도메인 키로 워크스페이스 번호 조회
    @Override
    public Long findByDomainKey(String domainKey) throws Exception {
        try {
            return mapper.findByDomainKey(domainKey);
        } catch (Exception e) {
            log.info("findByDomainKey : ", e);
        }
        return null;
    }

    // 프로젝트 등록
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertProject(ManageDto dto) throws SQLException {
        try {
            if (dto == null) {
                throw new IllegalArgumentException("프로젝트 정보가 없습니다.");
            }
            validateProject(
                    dto.getSubject(),
                    dto.getStart_date(),
                    dto.getEnd_date(),
                    dto.getStatus()
            );
            mapper.insertProject(dto);
        } catch (Exception e) {
            log.info("insertProject : ", e);
        }
    }

    // 프로젝트 목록 조회
    @Override
    public List<ManageDto> listProject(Map<String, Object> map) throws SQLException {
        try {
            return mapper.listProject(map);
        } catch (Exception e) {
            log.info("listProject : ", e);
        }
        return null;
    }

    // 프로젝트 데이터 개수 조회
    @Override
    public int dataCount(Map<String, Object> map) throws SQLException {
        try {
            return mapper.dataCount(map);
        } catch (Exception e) {
            log.info("dataCount : ", e);
        }
        return 0;
    }

    // 프로젝트 상세 정보 조회
    @Override
    public ManageDto getProjectDetail(Map<String, Object> map) throws SQLException {
        try {
            return mapper.getProjectDetail(map);
        } catch (Exception e) {
            log.info("getProjectDetail : ", e);
        }
        return null;
    }

    // 프로젝트 정보 수정
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProject(Map<String, Object> map) throws SQLException {
        try {
            checkProjectAccess(map);
            String subject = (String) map.get("subject");
            String startDate = (String) map.get("start_date");
            String endDate = (String) map.get("end_date");
            Integer status = (Integer) map.get("status");

            validateProject(subject, startDate, endDate, status);

            int result = mapper.updateProject(map);
            if (result != 1) {
                throw new IllegalStateException("프로젝트 수정에 실패했습니다.");
            }
        } catch (Exception e) {
            log.info("updateProject : ", e);
        }
    }

    // 프로젝트 삭제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Map<String, Object> map) throws SQLException {
        try {
            checkProjectAccess(map);
            int result = mapper.deleteProject(map);
            if (result != 1) {
                throw new IllegalStateException("프로젝트 삭제에 실패했습니다.");
            }
        } catch (Exception e) {
            log.info("deleteProject : ", e);
        }
    }

    // 프로젝트 유효성 검사
    private void validateProject(String subject, String startDate, String endDate, Integer status) {
        try {
            if (subject == null || subject.isBlank() || subject.length() > 200) {
                throw new IllegalArgumentException("프로젝트명을 확인하세요.");
            }
            if (status == null || status < 0 || status > 2) {
                throw new IllegalArgumentException("프로젝트 진행상태가 올바르지 않습니다.");
            }
            validateDates(startDate, endDate);
        } catch (Exception e) {
            log.info("validateProject : ", e);
        }
    }

    // 로그인한 사용자의 프로젝트 관리 권한 확인
    private void checkProjectAccess(Map<String, Object> map) throws SQLException {
        try {
            if (map == null || map.get("projectId") == null || map.get("domainKey") == null || map.get("memberId") == null) {
                throw new SecurityException("프로젝트 관리에 필요한 정보가 없습니다.");
            }

            Integer role = mapper.getProjectMemberRole(map);

            if (role == null) {
                return;
            }

            if (role >= 70) {
                return; 
            }

            if (role >= 50) {
                if (map.get("taskId") != null) {
                    int count = mapper.countProjectAccess(map);
                    if (count > 0) {
                        return;
                    } else {
                        throw new SecurityException("본인이 작성한 업무만 지울 수 있습니다.");
                    }
                } 
                else {
                    throw new SecurityException("프로젝트 자체의 삭제 및 수정은 PM만 가능합니다.");
                }
            }

            throw new SecurityException("해당 기능을 실행할 권한이 없습니다.");
            
        } catch (Exception e) {
            log.info("checkProjectAccess : ", e);
        }
    }

    // 날짜 유효성 검사
    private void validateDates(String startDate, String endDate) {
        try {
            if (startDate == null || endDate == null || startDate.isBlank() || endDate.isBlank()) {
                throw new IllegalArgumentException("시작일과 종료일을 입력하세요.");
            }

            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            if (start.isAfter(end)) {
                throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
            }
        } catch (Exception e) {
            log.info("validateDates : ", e);
        }
    }

    // 프로젝트 업무 목록 조회
    @Override
    public List<ManageTaskDto> listManageTasks(Map<String, Object> map) throws SQLException {
        try {
            return mapper.listManageTasks(map);
        } catch (Exception e) {
            log.info("listManageTasks : ", e);
        }
        return null;
    }

    // 담당 TL 목록 조회
    @Override
    public List<Map<String, Object>> listManageTaskTL(Map<String, Object> map) throws SQLException {
        try {
            return mapper.listManageTaskTL(map);
        } catch (Exception e) {
            log.info("listManageTaskTL : ", e);
        }
        return null;
    }

    // 업무 유효성 검사
    private void validateTask(ManageTaskDto dto) {
        try {
            if (dto == null) {
                throw new IllegalArgumentException("업무 정보가 없습니다.");
            }
            if (dto.getTitle() == null || dto.getTitle().isBlank() || dto.getTitle().length() > 200) {
                throw new IllegalArgumentException("업무명을 확인하세요.");
            }
            if (dto.getContent() == null || dto.getContent().isBlank()) {
                throw new IllegalArgumentException("업무 내용을 입력하세요.");
            }
            if (dto.getProject_member_id() == null) {
                throw new IllegalArgumentException("담당 TL을 선택하세요.");
            }
            
            if (dto.getTask_level() < 0 || dto.getTask_level() > 9) {
                throw new IllegalArgumentException("진행단계는 0~9단계여야 합니다.");
            }
            validateDates(dto.getStart_date(), dto.getEnd_date());
        } catch (Exception e) {
            log.info("validateTask : ", e);
        }
    }

    // 프로젝트 담당 TL 확인
    private void checkTaskTL(ManageTaskDto dto) throws SQLException {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("projectId", dto.getProject_id());
            map.put("projectMemberId", dto.getProject_member_id());

            int count = mapper.countValidTaskTL(map);

            if (count != 1) {
                throw new IllegalArgumentException("해당 프로젝트의 TL이 아닙니다.");
            }
        } catch (Exception e) {
            log.info("checkTaskTL : ", e);
        }
    }

    // 프로젝트 행 잠금
    private void lockProject(Map<String, Object> map) throws SQLException {
        try {
            Long projectId = mapper.lockManageProject(map);
            if (projectId == null) {
                throw new IllegalArgumentException("프로젝트가 존재하지 않습니다.");
            }
        } catch (Exception e) {
            log.info("lockProject : ", e);
        }
    }

    // 업무 보고서 등록 여부 확인
    private void checkTaskReports(Map<String, Object> map) throws SQLException {
        try {
            int count = mapper.countManageTaskReports(map);
            if (count > 0) {
                throw new IllegalStateException("보고서가 등록된 업무는 수정 또는 삭제할 수 없습니다.");
            }
        } catch (Exception e) {
            log.info("checkTaskReports : ", e);
        }
    }

    // 업무 등록
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertManageTask(ManageTaskDto dto, Map<String, Object> map) throws SQLException {
        try {
            checkProjectAccess(map);
            validateTask(dto);
            lockProject(map);
            checkTaskTL(dto);

            int result = mapper.insertManageTask(dto);
            if (result != 1) {
                throw new IllegalStateException("업무 등록에 실패했습니다.");
            }
        } catch (Exception e) {
            log.info("insertManageTask : ", e);
        }
    }

    // 업무 수정
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateManageTask(ManageTaskDto dto, Map<String, Object> map) throws SQLException {
        try {
            checkProjectAccess(map);
            validateTask(dto);
            lockProject(map);
            ManageTaskDto existing = mapper.getManageTask(map);

            if (existing == null) {
                throw new IllegalArgumentException("수정할 업무가 존재하지 않습니다.");
            }

            checkTaskReports(map);
            checkTaskTL(dto);

            int result = mapper.updateManageTask(dto);
            if (result != 1) {
                throw new IllegalStateException("업무 수정에 실패했습니다.");
            }
        } catch (Exception e) {
            log.info("updateManageTask : ", e);
        }
    }

    // 업무 삭제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteManageTask(Map<String, Object> map) throws SQLException {
        try {
            checkProjectAccess(map);
            lockProject(map);
            ManageTaskDto existing = mapper.getManageTask(map);

            if (existing == null) {
                throw new IllegalArgumentException("삭제할 업무가 존재하지 않습니다.");
            }

            checkTaskReports(map);
            int result = mapper.deleteManageTask(map);

            if (result != 1) {
                throw new IllegalStateException("업무 삭제에 실패했습니다.");
            }
        } catch (Exception e) {
            log.info("deleteManageTask : ", e);
        }
    }
}