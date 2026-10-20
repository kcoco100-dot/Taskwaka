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

	@Override
	public boolean canCreateProject(Long workspaceId, Long memberId) throws SQLException {
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("workspaceId", workspaceId);
			map.put("memberId", memberId);
			return mapper.countWorkspacePm(map) == 1;
		} catch (Exception e) {
			log.info("canCreateProject : ", e);
			throw e;
		}
	}

	// 도메인 키로 워크스페이스 번호 조회
	@Override
	public Long findByDomainKey(String domainKey) throws Exception {
		try {
			return mapper.findByDomainKey(domainKey);
		} catch (Exception e) {
			log.info("findByDomainKey : ", e);
			throw e;
		}
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

			Map<String, Object> access = new HashMap<>();
			access.put("workspaceId", dto.getWorkspace_id());
			access.put("memberId", dto.getMember_id());
			if (dto.getMember_id() == null || mapper.countWorkspacePm(access) != 1) {
				throw new SecurityException("프로젝트 생성은 해당 워크스페이스 PM만 가능합니다.");
			}
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
		try {
			return mapper.listProject(map);
		} catch (Exception e) {
			log.info("listProject : ", e);
			throw e;
		}
	}

	// 프로젝트 데이터 개수 조회
	@Override
	public int dataCount(Map<String, Object> map) throws SQLException {
		try {
			return mapper.dataCount(map);
		} catch (Exception e) {
			log.info("dataCount : ", e);
			throw e;
		}
	}

	// 프로젝트 상세 조회
	@Override
	public ManageDto getProjectDetail(Map<String, Object> map)
			throws SQLException {
		try {
			return mapper.getProjectDetail(map);
		} catch (Exception e) {
			log.info("getProjectDetail : ", e);
			throw e;
		}
	}

	// PM은 전체 프로젝트, DT는 본인 참여 프로젝트 관리
	@Override
	public boolean canManageProject(Map<String, Object> map) throws SQLException {
		try {
			if (map == null || map.get("projectId") == null || map.get("domainKey") == null || map.get("memberId") == null) {
				return false;
			}
			return mapper.countProjectAccess(map) == 1;
		} catch (Exception e) {
			log.info("canManageProject : ", e);
			throw e;
		}
	}

	// 프로젝트 수정
	@Override
	public void updateProject(Map<String, Object> map) throws SQLException {
		try {
			checkProjectAccess(map);
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

	// 프로젝트 삭제
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteProject(Map<String, Object> map) throws SQLException {
		try {
			checkProjectAccess(map);

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

	// 프로젝트 유효성 검사
	private void validateProject(
			String subject,
			String startDate,
			String endDate,
			Integer status) {

		try {
			if (subject == null
					|| subject.isBlank()
					|| subject.length() > 200) {
				throw new IllegalArgumentException("프로젝트명을 확인하세요.");
			}

			if (status == null || status < 0 || status > 2) {
				throw new IllegalArgumentException(
						"프로젝트 진행상태가 올바르지 않습니다."
				);
			}

			validateDates(startDate, endDate);

		} catch (Exception e) {
			log.info("validateProject : ", e);
			throw e;
		}
	}

	// 프로젝트 수정·삭제 권한 검사
	private void checkProjectAccess(Map<String, Object> map) throws SQLException {
		if (!canManageProject(map)) {
			throw new SecurityException("프로젝트 관리 권한이 없습니다.");
		}
	}

	// 업무 관리 권한 검사
	private void checkTaskManageAccess(Map<String, Object> map) throws SQLException {
		if (!canManageProject(map)) {
			throw new SecurityException("업무 관리 권한이 없습니다.");
		}
	}

	// 날짜 유효성 검사
	private void validateDates(String startDate, String endDate) {
		try {
			if (startDate == null
					|| endDate == null
					|| startDate.isBlank()
					|| endDate.isBlank()) {
				throw new IllegalArgumentException(
						"시작일과 종료일을 입력하세요."
				);
			}

			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(endDate);

			if (start.isAfter(end)) {
				throw new IllegalArgumentException(
						"종료일은 시작일보다 빠를 수 없습니다."
				);
			}

		} catch (Exception e) {
			log.info("validateDates : ", e);
			throw e;
		}
	}

	// 프로젝트 업무 목록
	@Override
	public List<ManageTaskDto> listManageTasks(Map<String, Object> map)
			throws SQLException {
		try {
			return mapper.listManageTasks(map);
		} catch (Exception e) {
			log.info("listManageTasks : ", e);
			throw e;
		}
	}

	// 업무 담당 TL 목록
	@Override
	public List<Map<String, Object>> listManageTaskTL(
			Map<String, Object> map) throws SQLException {

		try {
			return mapper.listManageTaskTL(map);
		} catch (Exception e) {
			log.info("listManageTaskTL : ", e);
			throw e;
		}
	}

	// 업무 입력값 검증
	private void validateTask(ManageTaskDto dto) {
		try {
			if (dto == null) {
				throw new IllegalArgumentException("업무 정보가 없습니다.");
			}

			if (dto.getTitle() == null
					|| dto.getTitle().isBlank()
					|| dto.getTitle().length() > 200) {
				throw new IllegalArgumentException("업무명을 확인하세요.");
			}

			if (dto.getContent() == null
					|| dto.getContent().isBlank()) {
				throw new IllegalArgumentException(
						"업무 내용을 입력하세요."
				);
			}

			if (dto.getProject_id() == null
					|| dto.getProject_member_id() == null) {
				throw new IllegalArgumentException(
						"프로젝트와 담당 TL을 확인하세요."
				);
			}

			if (dto.getTask_level() < 0
					|| dto.getTask_level() > 9) {
				throw new IllegalArgumentException(
						"진행단계가 올바르지 않습니다."
				);
			}

			validateDates(dto.getStart_date(), dto.getEnd_date());

		} catch (Exception e) {
			log.info("validateTask : ", e);
			throw e;
		}
	}

	// 프로젝트에 소속된 TL인지 확인
	private void checkTaskTL(ManageTaskDto dto) throws SQLException {
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("projectId", dto.getProject_id());
			map.put("projectMemberId", dto.getProject_member_id());

			int count = mapper.countValidTaskTL(map);

			if (count != 1) {
				throw new IllegalArgumentException(
						"해당 프로젝트의 활성 TL이 아닙니다."
				);
			}

		} catch (Exception e) {
			log.info("checkTaskTL : ", e);
			throw e;
		}
	}

	// 프로젝트 행 잠금
	private void lockProject(Map<String, Object> map) throws SQLException {
		try {
			Long projectId = mapper.lockManageProject(map);

			if (projectId == null) {
				throw new IllegalArgumentException(
						"프로젝트가 존재하지 않습니다."
				);
			}

		} catch (Exception e) {
			log.info("lockProject : ", e);
			throw e;
		}
	}

	// 업무 보고서 등록 여부 검사
	private void checkTaskReports(Map<String, Object> map)
			throws SQLException {

		try {
			int count = mapper.countManageTaskReports(map);

			if (count > 0) {
				throw new IllegalStateException(
						"보고서가 등록된 업무는 삭제할 수 없습니다."
				);
			}

		} catch (Exception e) {
			log.info("checkTaskReports : ", e);
			throw e;
		}
	}

	// 업무 등록 및 TL 참여 등록
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void insertManageTask(
			ManageTaskDto dto,
			Map<String, Object> map) throws SQLException {

		try {
			checkTaskManageAccess(map);
			validateTask(dto);
			lockProject(map);
			checkTaskTL(dto);

			if (!dto.getProject_id().equals(map.get("projectId"))) {
				throw new IllegalArgumentException(
						"프로젝트 정보가 일치하지 않습니다."
				);
			}

			int result = mapper.insertManageTask(dto);

			if (result != 1) {
				throw new IllegalStateException(
						"업무 등록에 실패했습니다."
				);
			}

			if (dto.getTask_id() == null) {
				throw new IllegalStateException("생성된 업무 번호가 없습니다.");
			}

			Map<String, Object> memberMap = new HashMap<>();
			memberMap.put("taskId", dto.getTask_id());
			memberMap.put(
					"projectMemberId",
					dto.getProject_member_id()
			);

			mapper.insertManageTaskMember(memberMap);

		} catch (Exception e) {
			log.info("insertManageTask : ", e);
			throw e;
		}
	}

	// 업무 수정 및 담당 TL 변경
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateManageTask(ManageTaskDto dto, Map<String, Object> map) throws SQLException {
		try {
			checkTaskManageAccess(map);
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
			int reportCount = mapper.countManageTaskReports(map);
			boolean otherChanged = !Objects.equals(existing.getTitle(), dto.getTitle())
					|| !Objects.equals(existing.getContent(), dto.getContent())
					|| existing.getTask_level() != dto.getTask_level()
					|| !Objects.equals(existing.getStart_date(), dto.getStart_date())
					|| !Objects.equals(existing.getEnd_date(), dto.getEnd_date());
			int result;
			if (reportCount > 0) {
				if (otherChanged) {
					throw new IllegalStateException("보고서가 등록된 업무는 담당 TL만 변경할 수 있습니다.");
				}
				Map<String, Object> updateMap = new HashMap<>();
				updateMap.put("taskId", dto.getTask_id());
				updateMap.put("projectId", dto.getProject_id());
				updateMap.put("projectMemberId", dto.getProject_member_id());
				result = mapper.updateManageTaskTL(updateMap);
			} else {
				result = mapper.updateManageTask(dto);
			}
			if (result != 1) {
				throw new IllegalStateException("업무 수정에 실패했습니다.");
			}
			Map<String, Object> memberMap = new HashMap<>();
			memberMap.put("taskId", dto.getTask_id());
			memberMap.put("projectMemberId", dto.getProject_member_id());
			mapper.insertManageTaskMember(memberMap);
			if (!Objects.equals(existing.getProject_member_id(), dto.getProject_member_id())) {
				memberMap.put("projectMemberId", existing.getProject_member_id());
				mapper.deleteManageTaskMember(memberMap);
			}
		} catch (Exception e) {
			log.info("updateManageTask : ", e);
			throw e;
		}
	}

	// 업무 삭제 및 TL 참여 정리
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteManageTask(Map<String, Object> map)
			throws SQLException {

		try {
			checkTaskManageAccess(map);
			lockProject(map);

			ManageTaskDto existing = mapper.getManageTask(map);

			if (existing == null) {
				throw new IllegalArgumentException(
						"삭제할 업무가 존재하지 않습니다."
				);
			}

			checkTaskReports(map);

			int result = mapper.deleteManageTask(map);

			if (result != 1) {
				throw new IllegalStateException(
						"업무 삭제에 실패했습니다."
				);
			}

			Map<String, Object> memberMap = new HashMap<>();
			memberMap.put("taskId", existing.getTask_id());
			memberMap.put(
					"projectMemberId",
					existing.getProject_member_id()
			);

			mapper.deleteManageTaskMember(memberMap);

		} catch (Exception e) {
			log.info("deleteManageTask : ", e);
			throw e;
		}
	}
}
