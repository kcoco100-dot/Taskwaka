package com.tk.app.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageTaskDto;
import com.tk.app.mapper.ManageProjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageProjectServiceImpl implements ManageProjectService {

	private final ManageProjectMapper mapper;
	private final GrowthTreeService growthTreeService;
	
	// 등록 버튼 표시 조건 조회
	@Override
	public boolean canCreateProject(Long workspaceId, Long memberId) throws SQLException {
		boolean result = false;
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("workspaceId", workspaceId);
			map.put("memberId", memberId);
			result = mapper.countWorkspacePm(map) == 1;
		} catch (Exception e) {
			log.info("canCreateProject : ", e);
			
			throw e;
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
			
			throw e;
		}
	}

	// 프로젝트 목록 조회
	@Override
	public List<ManageDto> listProject(Map<String, Object> map)
			throws SQLException {
		List<ManageDto> result = new java.util.ArrayList<>();
		try {
			result = mapper.listProject(map);
		} catch (Exception e) {
			log.info("listProject : ", e);
			
			throw e;
		}
		return result;
	}

	// 프로젝트 건수 조회
	@Override
	public int dataCount(Map<String, Object> map) throws SQLException {
		int result = 0;
		try {
			result = mapper.dataCount(map);
		} catch (Exception e) {
			log.info("dataCount : ", e);
			
			throw e;
		}
		
		return result;
	}

	// 프로젝트 상세 조회
	@Override
	public ManageDto getProjectDetail(Map<String, Object> map)
			throws SQLException {
		ManageDto result = null;
		try {
			result = mapper.getProjectDetail(map);
		} catch (Exception e) {
			log.info("getProjectDetail : ", e);
			
			throw e;
		}
		return result;
	}

	// 수정 버튼 표시 조건 조회
	@Override
	public boolean canManageProject(Map<String, Object> map) throws SQLException {
		boolean result = false;
		try {
			result = mapper.countProjectAccess(map) == 1;
		} catch (Exception e) {
			log.info("canManageProject : ", e);
			
			throw e;
		}
		return result;
	}

	// 프로젝트 수정
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateProject(Map<String, Object> map) throws SQLException {
		try {
			lockProject(map);
			validateProject((String) map.get("subject"), (String) map.get("start_date"),
					(String) map.get("end_date"), (Integer) map.get("status"));
			int result = mapper.updateProject(map);
			if (result != 1) {
				throw new IllegalStateException("프로젝트 수정에 실패했습니다.");
			}
		} catch (Exception e) {
			log.info("updateProject : ", e);
			throw e;
		}
	}

	// 프로젝트 숨김
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteProject(Map<String, Object> map) throws SQLException {
		try {
			lockProject(map);

			ManageDto existing = mapper.getProjectDetail(map);

			if (existing == null) {
				throw new IllegalArgumentException("프로젝트가 존재하지 않습니다.");
			}

			mapper.deleteProject(map);

		} catch (Exception e) {
			log.info("deleteProject : ", e);
			throw e;
		}
	}

	// 프로젝트 입력값 확인
	private void validateProject(
			String subject,
			String startDate,
			String endDate,
			Integer status) {

		try {
			if (subject == null
					|| subject.isBlank()
					|| subject.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 200) {
				throw new IllegalArgumentException("프로젝트명을 확인하세요.");
			}

			if (status == null || status < 0 || status > 2) {
				throw new IllegalArgumentException("프로젝트 진행상태가 올바르지 않습니다.");
			}

			validateDates(startDate, endDate);

		} catch (Exception e) {
			log.info("validateProject : ", e);
			throw e;
		}
	}

	// 기간 입력값 확인
	private void validateDates(String startDate, String endDate) {
		try {
			if (startDate == null
					|| endDate == null
					|| startDate.isBlank()
					|| endDate.isBlank()) {
				throw new IllegalArgumentException("시작일과 종료일을 입력하세요.");
			}

			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);

			if (start.isAfter(end)) {
				throw new IllegalArgumentException("종료일은 시작일보다 빠를 수 없습니다.");
			}

		} catch (Exception e) {
			log.info("validateDates : ", e);
			throw e;
		}
	}

	// 프로젝트 업무 목록 조회
	@Override
	public List<ManageTaskDto> listManageTasks(Map<String, Object> map)
			throws SQLException {
		List<ManageTaskDto> result = new java.util.ArrayList<>();
		try {
			result = mapper.listManageTasks(map);
		} catch (Exception e) {
			log.info("listManageTasks : ", e);
			throw e;
		}
		return result;
	}

	// 업무 담당자 목록 조회
	@Override
	public List<Map<String, Object>> listManageTaskTL(
			Map<String, Object> map) throws SQLException {
		List<Map<String, Object>> result = new java.util.ArrayList<>();

		try {
			result = mapper.listManageTaskTL(map);
			
		} catch (Exception e) {
			log.info("listManageTaskTL : ", e);
			
			throw e;
		}
		return result;
	}

	// 업무 입력값 확인
	private void validateTask(ManageTaskDto dto) {
		try {
			if (dto == null) {
				throw new IllegalArgumentException("업무 정보가 없습니다.");
			}

			if (dto.getTitle() == null
					|| dto.getTitle().isBlank()
					|| dto.getTitle().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 200) {
				throw new IllegalArgumentException("업무명을 확인하세요.");
			}

			if (dto.getContent() == null
					|| dto.getContent().isBlank()) {
				throw new IllegalArgumentException("업무 내용을 입력하세요.");
			}

			if (dto.getProject_id() == null
					|| dto.getProject_member_id() == null) {
				throw new IllegalArgumentException("프로젝트와 담당 TL을 확인하세요.");
			}

			if (dto.getTask_level() < 0
					|| dto.getTask_level() > 5) {
				throw new IllegalArgumentException("진행단계가 올바르지 않습니다.");
			}

			validateDates(dto.getStart_date(), dto.getEnd_date());

		} catch (Exception e) {
			log.info("validateTask : ", e);
			
			throw e;
		}
	}

	// 업무 담당자 조건 확인
	private void checkTaskTL(ManageTaskDto dto) throws SQLException {
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("projectId", dto.getProject_id());
			map.put("projectMemberId", dto.getProject_member_id());

			int count = mapper.countValidTaskTL(map);

			if (count != 1) {
				throw new IllegalArgumentException("해당 프로젝트의 활성 TL 이상 담당자가 아닙니다.");
			}

		} catch (Exception e) {
			log.info("checkTaskTL : ", e);
			
			throw e;
		}
	}

	// 프로젝트 변경 잠금
	private void lockProject(Map<String, Object> map) throws SQLException {
		try {
			Long projectId = mapper.lockManageProject(map);

			if (projectId == null) {
				throw new IllegalArgumentException("프로젝트가 존재하지 않습니다.");
			}

		} catch (Exception e) {
			log.info("lockProject : ", e);
			
			throw e;
		}
	}

	// 업무 등록
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void insertManageTask(
			ManageTaskDto dto,
			Map<String, Object> map) throws SQLException {

		try {
			validateTask(dto);
			lockProject(map);
			checkTaskTL(dto);

			if (!dto.getProject_id().equals(map.get("projectId"))) {
				throw new IllegalArgumentException("프로젝트 정보가 일치하지 않습니다.");
			}

			checkTaskWeight(dto, map);
			int result = mapper.insertManageTask(dto);

			if (result != 1) {
				throw new IllegalStateException("업무 등록에 실패했습니다.");
			}

			if (dto.getTask_id() == null) {
				throw new IllegalStateException("생성된 업무 번호가 없습니다.");
			}

			Map<String, Object> memberMap = new HashMap<>();
			memberMap.put("taskId", dto.getTask_id());
			memberMap.put("projectMemberId", dto.getProject_member_id());

			mapper.insertManageTaskMember(memberMap);
			mapper.refreshProjectProgress(map);

			growthTreeService.grantProjectCompleteExp(((Number)map.get("projectId")).longValue());
			
		} catch (Exception e) {
			log.info("insertManageTask : ", e);
			
			throw e;
		}
	}

	// 업무 수정
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateManageTask(ManageTaskDto dto, Map<String, Object> map) throws SQLException {
		try {
			validateTask(dto);
			lockProject(map);
			if (dto.getTask_id() == null || !dto.getProject_id().equals(map.get("projectId"))
					|| !dto.getTask_id().equals(map.get("taskId"))) {
				throw new IllegalArgumentException("업무 정보가 일치하지 않습니다.");
			}
			
			ManageTaskDto existing = mapper.getManageTask(map);
			if (existing == null) {
				throw new IllegalArgumentException("수정할 업무가 존재하지 않습니다.");
			}
			
			checkTaskTL(dto);
			checkTaskWeight(dto, map);
			
			int result = mapper.updateManageTask(dto);
			
			if (result != 1) {
				throw new IllegalStateException("업무 수정에 실패했습니다.");
			}
			Map<String, Object> memberMap = new HashMap<>();
			
			memberMap.put("taskId", dto.getTask_id());
			memberMap.put("projectMemberId", dto.getProject_member_id());
			
			mapper.insertManageTaskMember(memberMap);
			mapper.refreshProjectProgress(map);
			
			if (!Objects.equals(existing.getProject_member_id(), dto.getProject_member_id())) {
				memberMap.put("projectMemberId", existing.getProject_member_id());
				mapper.deleteManageTaskMember(memberMap);
			}
			
			growthTreeService.grantProjectCompleteExp(((Number)map.get("projectId")).longValue());
			
		} catch (Exception e) {
			log.info("updateManageTask : ", e);
			throw e;
		}
	}

	// 업무 숨김
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteManageTask(Map<String, Object> map)
			throws SQLException {

		try {
			lockProject(map);

			ManageTaskDto existing = mapper.getManageTask(map);

			if (existing == null) {
				throw new IllegalArgumentException(
						"삭제할 업무가 존재하지 않습니다."
				);
			}

			int result = mapper.deleteManageTask(map);

			if (result != 1) {
				throw new IllegalStateException(
						"업무 삭제에 실패했습니다."
				);
			}

			mapper.refreshProjectProgress(map);
			
			growthTreeService.grantProjectCompleteExp(((Number)map.get("projectId")).longValue());
			
		} catch (Exception e) {
			log.info("deleteManageTask : ", e);
			throw e;
		}
	}

    // 업무 배정 비중 확인
    private void checkTaskWeight(ManageTaskDto dto, Map<String, Object> map) throws SQLException {
        Double weight = dto.getProgress_weight();
        if (weight == null || !Double.isFinite(weight) || weight < 1 || weight > 100 || weight != Math.rint(weight)) {
            throw new IllegalArgumentException("배정 비중은 1부터 100까지 정수로 입력하세요.");
        }

        if (mapper.sumTaskWeights(map) + weight > 100.000001) {
            throw new IllegalArgumentException("업무 배정 비중의 합계는 100%를 초과할 수 없습니다.");
        }
    }

    // 이전 프로젝트 조회
    @Override
    public ManageDto selectPrevProject(Map<String, Object> map) throws SQLException {
        ManageDto result = null;
        try {
            result = mapper.selectPrevProject(map);
        } catch (Exception e) {
            log.info("selectPrevProject : ", e);
            throw e;
        }
        return result;
    }
    
    // 다음 프로젝트 조회
    @Override
    public ManageDto selectNextProject(Map<String, Object> map) throws SQLException {
        ManageDto result = null;
        try {
            result = mapper.selectNextProject(map);
        } catch (Exception e) {
            log.info("selectNextProject : ", e);
            throw e;
        }
        return result;
    }
}
