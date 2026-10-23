package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.CalendarEventDto;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.DashboardSummaryDto;
import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.domain.dto.ProjectDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;
import com.tk.app.domain.dto.TeamReportDto;
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
    
    // 대시보드용
    DashboardSummaryDto getDashboardSummary(Long projectId);
    List<SubTaskDto> getUpcomingSubTasks(Long projectId);
    List<NoticeDto> getRecentProjectNotices(Long projectId);

    // 캘린더용
    List<CalendarEventDto> getCalendarEvents(Long projectId);
    CalendarEventDto createPersonalEvent(com.tk.app.domain.dto.CalendarEventDto dto);
    boolean deletePersonalEvent(Long scheduleId);
    
    // 업무공지 탭 전용
    boolean isTeamLeaderOrAbove(Long projectMemberId);
    Long getMemberIdByProjectMemberId(Long projectMemberId);
    List<Map<String, Object>> getTaskNoticeList(Long projectId, boolean isLeader);
    Map<String, Object> getTaskNoticeDetail(Long noticeId, boolean incrementView);
    Map<String, Object> createTaskNotice(java.util.Map<String, Object> param);
    void updateTaskNotice(java.util.Map<String, Object> param);
    void deleteTaskNotice(Long noticeId);
    
    public void createTeamReport(TeamReportDto dto);
}