package com.tk.app.service;

import com.tk.app.domain.dto.CalendarEventDto;
import com.tk.app.domain.dto.DashboardSummaryDto;
import com.tk.app.domain.dto.NoticeDto;
import java.math.RoundingMode;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.common.ManageFileStorage;
import com.tk.app.common.MyUtil;
import com.tk.app.security.LoginMemberUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.ProjectDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;
import com.tk.app.domain.dto.TeamReportDto;
import com.tk.app.mapper.ProjectMapper;
import com.tk.app.mapper.TaskMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ManageFileStorage fileStorage;
    
    @Value("${file.upload-root}/task")
	private String uploadPath;

    @Override
    @Transactional(readOnly = true)
    public List<TaskDto> getTaskList(Long projectId) {
        List<TaskDto> tasks = taskMapper.selectTasksByProjectId(projectId);
        if (!tasks.isEmpty()) {
            var children = taskMapper.selectSubTasksByProjectId(projectId).stream().collect(Collectors.groupingBy(SubTaskDto::getTaskId));
            tasks.forEach(task -> task.setSubTasks(children.getOrDefault(task.getTaskId(), List.of())));
        }
        return tasks;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTaskDetail(Long taskId) {
        TaskDto task = taskMapper.selectTaskById(taskId);
        if (task != null) {
            task.setSubTasks(taskMapper.selectSubTasksByTaskId(taskId));
            task.setParticipants(getSelectableMembersForTask(task.getProjectId(), taskId));
        }
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public SubTaskDto getSubTaskDetail(Long subTaskId) {
        SubTaskDto sub = taskMapper.selectSubTaskById(subTaskId);
        if (sub != null) {
            sub.setFiles(taskMapper.subTaskFiles(subTaskId));
            sub.setParticipants(getSelectableMembersForSubTask(sub.getProjectId(), sub.getTaskId(), subTaskId));
        }
        return sub;
    }

    @Override
    public void updateTaskDetail(TaskDto dto) {
        requireChanged(taskMapper.updateTaskDetail(dto));
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
        requireChanged(taskMapper.updateSubTaskDetail(dto));
        try {
            for (var file : fileStorage.save(dto.getSelectFile(), uploadPath)) {
                requireChanged(taskMapper.insertSubTaskFile(dto.getSubTaskId(), file));
            }
        } catch (Exception e) {
            throw new IllegalStateException("하위 업무 첨부를 저장하지 못했습니다.", e);
        }
    }

    private static void requireChanged(int count) {
        if (count != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "대상이 없거나 이미 변경됐습니다.");
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
        requireChanged(taskMapper.softDeleteSubTask(subTaskId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskMemberDto> getSelectableMembersForTask(Long projectId, Long taskId) {
        List<TaskMemberDto> list = taskMapper.selectProjectMembers(projectId);
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
    public List<DailyReportDto> getDailyReports(Long subTaskId, long authorId, boolean leader) {
        return taskMapper.selectDailyReportsBySubTaskId(subTaskId, authorId, leader);
    }

    @Override
    @Transactional
    public DailyReportDto createDailyReport(DailyReportDto dto) {
        double delta = validDelta(dto.getProgress());
        double progress = nextProgress(dto.getSubTaskId(), delta);
        requireChanged(taskMapper.insertDailyReport(dto));
        requireChanged(taskMapper.updateSubTaskField(dto.getSubTaskId(), "progress", progress));
        return taskMapper.selectDailyReportById(dto.getDailyReportId());
    }

    private static double validDelta(Double delta) {
        if (delta == null || !Double.isFinite(delta) || delta < 0 || delta > 100 ||
                BigDecimal.valueOf(delta).stripTrailingZeros().scale() > 2)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "증가분은 0~100 사이 소수 둘째 자리까지 입력하세요.");
        return delta;
    }

    private double nextProgress(long subTaskId, double change) {
        Double current = taskMapper.lockSubTaskProgress(subTaskId);
        if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        double next = BigDecimal.valueOf(current).add(BigDecimal.valueOf(change)).setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (next < 0 || next > 100) throw new ResponseStatusException(HttpStatus.CONFLICT, "합산 진행률은 0~100 사이여야 합니다.");
        return next;
    }

    @Override
    public boolean updateDailyReport(DailyReportDto dto) {
        DailyReportDto old = taskMapper.lockDailyReport(dto.getDailyReportId(), LoginMemberUtil.getSessionInfo().getMember_id());
        if (old == null) return false;
        dto.setSubTaskId(old.getSubTaskId());
        double progress = nextProgress(old.getSubTaskId(), validDelta(dto.getProgress()) - old.getProgress());
        if (taskMapper.updateDailyReport(dto) != 1) return false;
        requireChanged(taskMapper.updateSubTaskField(old.getSubTaskId(), "progress", progress));
        return true;
    }

    @Override
    public boolean deleteDailyReport(Long dailyReportId) {
        DailyReportDto old = taskMapper.lockDailyReport(dailyReportId, LoginMemberUtil.getSessionInfo().getMember_id());
        if (old == null) return false;
        double progress = nextProgress(old.getSubTaskId(), -old.getProgress());
        if (taskMapper.softDeleteDailyReport(dailyReportId) != 1) return false;
        requireChanged(taskMapper.updateSubTaskField(old.getSubTaskId(), "progress", progress));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDto getProjectById (Long projectId) {
    	return projectMapper.selectProjectById(projectId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary(Long projectId) {
        return taskMapper.selectProjectDashboardSummary(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubTaskDto> getUpcomingSubTasks(Long projectId) {
        return taskMapper.selectUpcomingSubTasks(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoticeDto> getRecentProjectNotices(Long projectId) {
        return taskMapper.selectRecentProjectNotices(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getCalendarEvents(Long projectId, long workspaceMemberId) {
        return taskMapper.selectCalendarEventsByProject(projectId, workspaceMemberId);
    }

    @Override
    public CalendarEventDto createPersonalEvent(CalendarEventDto dto) {
        dto.setScheduleType("PERSONAL");
        taskMapper.insertCalendarEvent(dto);
        return dto;
    }

    @Override
    public boolean deletePersonalEvent(Long scheduleId, long projectId, long workspaceMemberId) {
        return taskMapper.deleteCalendarEvent(scheduleId, projectId, workspaceMemberId) > 0;
    }

	@Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTaskNoticeList(Map<String, Object> map) {
        return taskMapper.selectTaskNoticesByProject(map);
    }
    
    @Override
    public int getTaskNoticeCount(Map<String, Object> map) {
    	return taskMapper.countTaskNoticesByProject(map);
    }

    @Override
    public Map<String, Object> getTaskNoticeDetail(Long noticeId, boolean incrementView) {
        if (incrementView) {
            taskMapper.incrementNoticeViewCount(noticeId);
        }
        var detail = taskMapper.selectTaskNoticeDetail(noticeId);
        if (detail != null && detail.get("content") instanceof String content) {
            detail.put("content", MyUtil.sanitize(content));
        }
        return detail;
    }

    @Override
    public Map<String, Object> createTaskNotice(Map<String, Object> param) {
        taskMapper.insertTaskNotice(param);
        Long noticeId = Long.valueOf(String.valueOf(param.get("noticeId")));
        return getTaskNoticeDetail(noticeId, false);
    }

    @Override
    public void updateTaskNotice(Map<String, Object> param) {
        requireChanged(taskMapper.updateTaskNotice(param));
    }

    @Override
    public void deleteTaskNotice(long projectId, Long noticeId) {
        requireChanged(taskMapper.softDeleteTaskNotice(projectId, noticeId));
    }
    
    @Override
    @Transactional
    public void createTeamReport(TeamReportDto dto) {
    	Long taskId = dto.getTaskId();
    	TaskDto taskDto = taskMapper.selectTaskById(taskId);
    	Long projectMemberId = taskDto.getProjectMemberId();
    	dto.setProjectMemberId(projectMemberId);
    	Double currentProgress = taskDto.getProgress();
    	Double addProgress = dto.getProgress();
    	taskMapper.insertTeamReport(dto);
    	//taskMapper.updateTaskField(taskId, "progress", currentProgress + addProgress);
    }
}
