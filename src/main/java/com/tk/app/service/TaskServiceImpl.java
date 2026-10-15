package com.tk.app.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.ProjectDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;
import com.tk.app.mapper.ProjectMapper;
import com.tk.app.mapper.TaskMapper;
import com.tk.app.service.TaskService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTaskList(Long projectId) {
        return taskMapper.selectTasksByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskDetail(Long taskId) {
        TaskDto task = taskMapper.selectTaskById(taskId);
        if (task != null) {
            task.setParticipants(getSelectableMembersForTask(task.getProjectId(), taskId));
        }
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public SubTaskDto getSubTaskDetail(Long subTaskId) {
        SubTaskDto sub = taskMapper.selectSubTaskById(subTaskId);
        if (sub != null) {
            sub.setParticipants(getSelectableMembersForSubTask(sub.getProjectId(), sub.getTaskId(), subTaskId));
        }
        return sub;
    }

    @Override
    public void updateTaskDetail(TaskDto dto) {
        taskMapper.updateTaskDetail(dto);
    }

    @Override
    public void updateTaskField(Long taskId, String fieldName, Object fieldValue) {
        taskMapper.updateTaskField(taskId, fieldName, fieldValue);
    }

    @Override
    public SubTaskDto createSubTask(SubTaskDto dto) {
        if (dto.getStatus() == null) dto.setStatus(0);
        if (dto.getProgress() == null) dto.setProgress(0.0);
        if (dto.getContent() == null || dto.getContent().isBlank()) dto.setContent(" ");

        // 1. 상위 업무 참여자(task_members) 테이블에 해당 담당자 보장 (복합 FK 만족)
        taskMapper.insertTaskMember(dto.getTaskId(), dto.getProjectMemberId());

        // 2. 하위업무(sub_tasks) 등록
        taskMapper.insertSubTask(dto);

        // 3. 하위업무 참여자(sub_task_members) 테이블에도 담당자 자동 등록
        taskMapper.insertSubTaskMember(dto.getSubTaskId(), dto.getTaskId(), dto.getProjectMemberId());

        return taskMapper.selectSubTaskById(dto.getSubTaskId());
    }

    @Override
    public void updateSubTaskDetail(SubTaskDto dto) {
        taskMapper.updateSubTaskDetail(dto);
    }

    @Override
    public void updateSubTaskField(Long subTaskId, String fieldName, Object fieldValue) {
        if ("projectMemberId".equals(fieldName) && fieldValue != null) {
            SubTaskDto sub = taskMapper.selectSubTaskById(subTaskId);
            if (sub != null) {
                Long newMemberId = Long.valueOf(String.valueOf(fieldValue));
                taskMapper.insertTaskMember(sub.getTaskId(), newMemberId);
                taskMapper.insertSubTaskMember(subTaskId, sub.getTaskId(), newMemberId);
            }
        }
        taskMapper.updateSubTaskField(subTaskId, fieldName, fieldValue);
    }

    @Override
    public void deleteSubTask(Long subTaskId) {
        taskMapper.softDeleteSubTask(subTaskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskMemberDto> getSelectableMembersForTask(Long projectId, Long taskId) {
        List<TaskMemberDto> list = taskMapper.selectTaskMembers(taskId);
        if (list == null || list.isEmpty()) {
            list = taskMapper.selectProjectMembers(projectId);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskMemberDto> getSelectableMembersForSubTask(Long projectId, Long taskId, Long subTaskId) {
        List<TaskMemberDto> list = taskMapper.selectSubTaskMembers(subTaskId);
        if (list == null || list.isEmpty()) {
            list = taskMapper.selectTaskMembers(taskId);
        }
        if (list == null || list.isEmpty()) {
            list = taskMapper.selectProjectMembers(projectId);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyReportDto> getDailyReports(Long subTaskId) {
        return taskMapper.selectDailyReportsBySubTaskId(subTaskId);
    }

    @Override
    public DailyReportDto createDailyReport(DailyReportDto dto) {
        if (dto.getProgress() == null) dto.setProgress(0.0);
        taskMapper.insertDailyReport(dto);
        // 일일보고서 진행률을 하위업무 진행률에도 동기화 반영
        taskMapper.updateSubTaskField(dto.getSubTaskId(), "progress", dto.getProgress());
        return taskMapper.selectDailyReportById(dto.getDailyReportId());
    }

    @Override
    public boolean updateDailyReport(DailyReportDto dto) {
        int updated = taskMapper.updateDailyReport(dto);
        if (updated > 0 && dto.getProgress() != null && dto.getSubTaskId() != null) {
            taskMapper.updateSubTaskField(dto.getSubTaskId(), "progress", dto.getProgress());
        }
        return updated > 0;
    }

    @Override
    public boolean deleteDailyReport(Long dailyReportId) {
        return taskMapper.softDeleteDailyReport(dailyReportId) > 0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById (Long projectId) {
    	return projectMapper.selectProjectById(projectId);
    }
}