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

    List<DailyReportDto> getDailyReports(Long subTaskId, long authorId, boolean leader);
    DailyReportDto createDailyReport(DailyReportDto dto);
    boolean updateDailyReport(DailyReportDto dto);
    boolean deleteDailyReport(Long dailyReportId);
    
    ProjectDto getProjectById (Long projectId);
    
    // 대시보드용
    DashboardSummaryDto getDashboardSummary(Long projectId);
    List<SubTaskDto> getUpcomingSubTasks(Long projectId);
    List<NoticeDto> getRecentProjectNotices(Long projectId);

    // 캘린더용
    List<CalendarEventDto> getCalendarEvents(Long projectId, long workspaceMemberId);
    CalendarEventDto createPersonalEvent(CalendarEventDto dto);
    boolean deletePersonalEvent(Long scheduleId, long projectId, long workspaceMemberId);
    
    // 업무공지 탭 전용
	List<Map<String, Object>> getTaskNoticeList(Map<String, Object> map);
    public int getTaskNoticeCount(Map<String, Object> map);
    Map<String, Object> getTaskNoticeDetail(Long noticeId, boolean incrementView);
    Map<String, Object> createTaskNotice(Map<String, Object> param);
    void updateTaskNotice(Map<String, Object> param);
    void deleteTaskNotice(long projectId, Long noticeId);
    
    public void createTeamReport(TeamReportDto dto);
}