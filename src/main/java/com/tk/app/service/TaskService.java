package com.tk.app.service;

import java.util.List;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.ProjectDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;
import com.tk.app.mapper.ProjectMapper;

public interface TaskService {
    List<TaskDto> getTaskList(Long projectId);
    TaskDto getTaskDetail(Long taskId);
    SubTaskDto getSubTaskDetail(Long subTaskId);

    void updateTaskDetail(TaskDto dto);
    void updateTaskField(Long taskId, String fieldName, Object fieldValue);

    SubTaskDto createSubTask(SubTaskDto dto);
    void updateSubTaskDetail(SubTaskDto dto);
    void updateSubTaskField(Long subTaskId, String fieldName, Object fieldValue);
    void deleteSubTask(Long subTaskId);

    List<TaskMemberDto> getSelectableMembersForTask(Long projectId, Long taskId);
    List<TaskMemberDto> getSelectableMembersForSubTask(Long projectId, Long taskId, Long subTaskId);

    List<DailyReportDto> getDailyReports(Long subTaskId);
    DailyReportDto createDailyReport(DailyReportDto dto);
    boolean updateDailyReport(DailyReportDto dto);
    boolean deleteDailyReport(Long dailyReportId);
    
    ProjectDto getProjectById (Long projectId);
}