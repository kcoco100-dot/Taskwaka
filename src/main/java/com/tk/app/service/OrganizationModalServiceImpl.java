package com.tk.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.OrganizationModalDto;
import com.tk.app.domain.dto.OrganizationTaskDto;
import com.tk.app.mapper.OrganizationModalMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizationModalServiceImpl implements OrganizationModalService {

	private final OrganizationModalMapper mapper;

	private void checkWork(OrganizationModalDto dto) {
		if(dto == null || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
				|| dto.getMemberId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "워크 정보가 올바르지 않습니다.");
		}

		Long workId = mapper.findActiveWorkId(dto);

		if(workId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "워크 접근 권한이 없습니다.");
		}

		dto.setWorkId(workId);
	}

	private void checkProject(OrganizationModalDto dto) {
		checkWork(dto);

		if(dto.getProjectId() == null || dto.getProjectId() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "프로젝트 정보가 올바르지 않습니다.");
		}

		if(mapper.countAccessibleProject(dto) == 0) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트 조회 권한이 없습니다.");
		}
	}

	@Override
	public List<OrganizationModalDto> findMyProjects(OrganizationModalDto dto) {
		checkWork(dto);
		return mapper.findMyProjects(dto);
	}

	private void addTaskTitle(Map<Long, TreeMap<Long, String>> titles,
			Long projectMemberId, Long taskId, String title) {

		if(projectMemberId == null || taskId == null || title == null || title.isBlank()) {
			return;
		}

		titles.computeIfAbsent(projectMemberId, key -> new TreeMap<>())
				.put(taskId, title);
	}

	@Override
	public Map<String, Object> findProjectTree(OrganizationModalDto dto) {
		checkProject(dto);

		List<OrganizationModalDto> members = mapper.findProjectMembers(dto);
		List<OrganizationTaskDto> tasks = mapper.findProjectTasks(dto);
		List<OrganizationTaskDto> subTasks = mapper.findProjectSubTasks(dto);

		Map<Long, TreeMap<Long, String>> taskTitles = new HashMap<>();
		Map<Long, TreeMap<Long, String>> subTaskTitles = new HashMap<>();
		Map<Long, OrganizationTaskDto> taskById = new HashMap<>();
		Map<Long, Long> subTaskParentIds = new HashMap<>();

		for(OrganizationTaskDto task : tasks) {
			taskById.put(task.getTaskId(), task);
		}

		for(OrganizationTaskDto subTask : subTasks) {
			subTaskParentIds.put(subTask.getSubTaskId(), subTask.getParentTaskId());
		}

		// 대표 담당자 업무
		for(OrganizationTaskDto task : tasks) {
			addTaskTitle(taskTitles, task.getProjectMemberId(), task.getTaskId(), task.getTitle());
		}

		// 추가 참여자 업무
		for(OrganizationTaskDto task : mapper.findTaskMembers(dto)) {
			addTaskTitle(taskTitles, task.getProjectMemberId(), task.getTaskId(), task.getTitle());
		}

		// 대표 담당자 하위업무
		for(OrganizationTaskDto subTask : subTasks) {
			addTaskTitle(subTaskTitles, subTask.getProjectMemberId(), subTask.getSubTaskId(), subTask.getTitle());
		}

		// 추가 참여자 하위업무
		for(OrganizationTaskDto subTask : mapper.findSubTaskMembers(dto)) {
			addTaskTitle(subTaskTitles, subTask.getProjectMemberId(), subTask.getSubTaskId(), subTask.getTitle());
		}

		// 하위업무만 배정된 회원도 해당 하위업무의 상위 업무를 표시
		for(Map.Entry<Long, TreeMap<Long, String>> entry : subTaskTitles.entrySet()) {
			Long projectMemberId = entry.getKey();

			for(Long subTaskId : entry.getValue().keySet()) {
				Long parentTaskId = subTaskParentIds.get(subTaskId);
				OrganizationTaskDto parentTask = taskById.get(parentTaskId);

				if(parentTask != null) {
					addTaskTitle(taskTitles, projectMemberId,
							parentTask.getTaskId(), parentTask.getTitle());
				}
			}
		}

		// SQL 집계 대신 서비스에서 참여자별 업무명 합치기
		for(OrganizationModalDto member : members) {
			TreeMap<Long, String> myTasks = taskTitles.get(member.getProjectMemberId());

			member.setTaskTitles(myTasks == null ? null : String.join(", ", myTasks.values()));
		}

		Map<String, Object> result = new HashMap<>();
		result.put("members", members);
		result.put("tasks", tasks);
		result.put("subTasks", subTasks);

		return result;
	}
}
