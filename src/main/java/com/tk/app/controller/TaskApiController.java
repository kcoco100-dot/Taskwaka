package com.tk.app.controller;

import com.tk.app.common.MyUtil;
import com.tk.app.domain.dto.CalendarEventDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;
import com.tk.app.domain.dto.TeamReportDto;
import com.tk.app.domain.dto.ProjectActor;
import com.tk.app.mapper.TaskMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tk.app.service.GrowthTreeService;
import com.tk.app.service.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{domainKey}/project/{projectId}/api/tasks")
@RequiredArgsConstructor
public class TaskApiController {

    private final TaskService taskService;
    private final ProjectAccessAdvice access;
    private final GrowthTreeService growthTreeService;
    private final TaskMapper taskMapper;
    private final StorageService storageService;

    private void checkAssignee(long projectId, Long assignee, ProjectActor actor) {
        if (assignee != null && (taskMapper.activeAssignee(projectId, assignee) != 1 ||
                (!actor.isLeader() && assignee != actor.projectMemberId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "지정할 수 없는 담당자입니다.");
        }
    }



    // 1. 전체 업무 목록 조회
    @GetMapping
    public ResponseEntity<List<TaskDto>> getTaskList(@PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(taskService.getTaskList(projectId));
    }

    // 2. 메인 업무 상세 단건 조회 (참여 인원 포함)
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskDetail(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    // 3. 하위 업무 상세 단건 조회 (우측 패널용, 참여 인원 포함)
    @GetMapping("/subtasks/{subTaskId}")
    public ResponseEntity<SubTaskDto> getSubTaskDetail(@PathVariable("subTaskId") Long subTaskId) {
        return ResponseEntity.ok(taskService.getSubTaskDetail(subTaskId));
    }

    // 4. 특정 메인 업무의 참여 인원(task_members) 목록 조회 -> 하위업무 생성 모달의 담당자 드롭다운용
    @GetMapping("/{taskId}/members")
    public ResponseEntity<List<TaskMemberDto>> getTaskMembers(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(taskService.getSelectableMembersForTask(projectId, taskId));
    }

    // 6. 메인 업무 상세 내용 저장
    @Transactional
    @PutMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> updateTaskDetail(
            @PathVariable("taskId") Long taskId,
            @PathVariable("projectId") Long projectId, @RequestAttribute("projectActor") ProjectActor actor,
            @RequestBody TaskDto dto) {
        access.lockMutation();
        checkAssignee(projectId, dto.getProjectMemberId(), actor);
        dto.setProjectId(projectId);
        dto.setTaskId(taskId);
        taskService.updateTaskDetail(dto);
        //storageService.uploadFileToServer(null, uploadPath);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

 // 8. 하위 업무 상세 내용 및 첨부파일 저장
    @Transactional
    @PutMapping("/subtasks/{subTaskId}")
    public ResponseEntity<Map<String, Object>> updateSubTaskDetail(
            @PathVariable("subTaskId") Long subTaskId,
            @PathVariable("projectId") Long projectId, @RequestAttribute("projectActor") ProjectActor actor,
            @ModelAttribute SubTaskDto dto) {
        access.lockMutation();
        checkAssignee(projectId, dto.getProjectMemberId(), actor);
        dto.setProjectId(projectId);

        dto.setSubTaskId(subTaskId);
        
        taskService.updateSubTaskDetail(dto);
        //res.put("files", taskService.getSubTaskFiles(subTaskId));

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("files", taskMapper.subTaskFiles(subTaskId));
        res.put("progress", taskMapper.lockSubTaskProgress(subTaskId));
        return ResponseEntity.ok(res);
    }

    // 9. 하위 업무 삭제
    @Transactional
    @DeleteMapping("/subtasks/{subTaskId}")
    public ResponseEntity<Map<String, Object>> deleteSubTask(@PathVariable("subTaskId") Long subTaskId) {
        access.lockMutation();
        taskService.deleteSubTask(subTaskId);
        //storageService.deleteFile(uploadPath, null);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/subtasks/{subTaskId}/files/{fileId}")
    public ResponseEntity<?> download(@PathVariable("subTaskId") long subTaskId, @PathVariable("fileId") long fileId) {
        var file = taskMapper.subTaskFile(subTaskId, fileId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return storageService.downloadFile(file.getPath(), file.getStoredName(), file.getOriginalName());
    }

    // 10. 일일 보고서 (daily_reports) CRUD API
    @GetMapping("/subtasks/{subTaskId}/daily-reports")
    public ResponseEntity<List<DailyReportDto>> getDailyReports(@PathVariable("subTaskId") Long subTaskId, @RequestAttribute("projectActor") ProjectActor actor) {
        return ResponseEntity.ok(taskService.getDailyReports(subTaskId, actor.projectMemberId(), actor.isLeader()));
    }

    @Transactional
    @PostMapping("/subtasks/{subTaskId}/daily-reports")
    public ResponseEntity<DailyReportDto> createDailyReport(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("subTaskId") Long subTaskId,
            @RequestBody DailyReportDto dto) {
        access.lockMutation();

        dto.setSubTaskId(subTaskId);

        return ResponseEntity.ok(
                growthTreeService.createDailyReportWithExp(dto, projectId, domainKey));
    }

    @Transactional
    @PutMapping("/daily-reports/{dailyReportId}")
    public ResponseEntity<Map<String, Object>> updateDailyReport(
            @PathVariable("dailyReportId") Long dailyReportId, @PathVariable("projectId") Long projectId,
            @PathVariable("domainKey") String domainKey, @RequestBody DailyReportDto dto) {
        access.lockMutation();
        dto.setDailyReportId(dailyReportId);
        growthTreeService.findDailyReportDeleteContext(dailyReportId, projectId, domainKey);
        boolean updated = taskService.updateDailyReport(dto);
        Map<String, Object> res = new HashMap<>();
        res.put("success", updated);
        if (!updated) {
            res.put("message", "상급자가 이미 열람/확인한 보고서는 수정할 수 없습니다.");
        }
        return ResponseEntity.status(updated ? 200 : 409).body(res);
    }

    @Transactional
    @DeleteMapping("/daily-reports/{dailyReportId}")
    public ResponseEntity<Map<String, Object>> deleteDailyReport(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("dailyReportId") Long dailyReportId) {
        access.lockMutation();

        boolean deleted = growthTreeService.deleteDailyReportWithExp(dailyReportId, projectId, domainKey);
        if (!deleted) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 열람했거나 삭제된 보고서입니다.");
        return ResponseEntity.ok(Map.of("success", true));
    }
    
    // 11. 캘린더 일정 (업무 연동 + 개인 일정 추가/삭제) API 
    @GetMapping("/calendar-events")
    public ResponseEntity<List<CalendarEventDto>> getCalendarEvents(
            @PathVariable("projectId") Long projectId, @RequestAttribute("projectActor") ProjectActor actor) {
        return ResponseEntity.ok(taskService.getCalendarEvents(projectId, actor.workspaceMemberId()));
    }

    @Transactional
    @PostMapping("/calendar-events")
    public ResponseEntity<CalendarEventDto> createCalendarEvent(
            @PathVariable("projectId") Long projectId,
            @RequestAttribute("projectActor") ProjectActor actor,
            @RequestBody CalendarEventDto dto) {
        access.lockMutation();
        dto.setWorkspaceId(actor.workspaceId());
        dto.setWorkspaceMemberId(actor.workspaceMemberId());
        dto.setProjectId(projectId);
        return ResponseEntity.ok(taskService.createPersonalEvent(dto));
    }

    @Transactional
    @DeleteMapping("/calendar-events/{scheduleId}")
    public ResponseEntity<Map<String, Object>> deleteCalendarEvent(
            @PathVariable("scheduleId") Long scheduleId, @PathVariable("projectId") Long projectId,
            @RequestAttribute("projectActor") ProjectActor actor) {
        access.lockMutation();
        boolean deleted = taskService.deletePersonalEvent(scheduleId, projectId, actor.workspaceMemberId());
        Map<String, Object> res = new HashMap<>();
        res.put("success", deleted);
        return ResponseEntity.ok(res);
    }
    
 // 12. 업무공지 (notice) CRUD API (팀 리더 전용 작성/수정/삭제)
    @GetMapping("/notices/{noticeId}")
    public ResponseEntity<Map<String, Object>> getTaskNoticeDetail(
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "incView", defaultValue = "true") boolean incView) {
        return ResponseEntity.ok(taskService.getTaskNoticeDetail(noticeId, incView));
    }

    private void prepareNotice(Map<String, Object> form, long projectId, ProjectActor actor) {
        if (!actor.isLeader()) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Object id = form.get("taskId");
        if (id == null || id.toString().isBlank()) {
            form.put("taskId", null);
            form.put("noticeType", "PROJECT");
        } else {
            long taskId;
            try { taskId = Long.parseLong(id.toString()); }
            catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST); }
            if (taskMapper.projectTask(projectId, taskId) != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            form.put("taskId", taskId);
            form.put("noticeType", "TASK");
        }
        form.put("content", MyUtil.sanitize((String) form.get("content")));
        form.put("memberId", actor.memberId());
        form.put("projectId", projectId);
    }

    @Transactional
    @PostMapping("/notices")
    public Map<String, Object> createTaskNotice(@PathVariable("projectId") long projectId,
            @RequestAttribute("projectActor") ProjectActor actor, @RequestBody Map<String, Object> form) {
        access.lockMutation();
        prepareNotice(form, projectId, actor);
        return Map.of("success", true, "notice", taskService.createTaskNotice(form));
    }

    @Transactional
    @PutMapping("/notices/{noticeId}")
    public Map<String, Object> updateTaskNotice(@PathVariable("projectId") long projectId, @PathVariable("noticeId") long noticeId,
            @RequestAttribute("projectActor") ProjectActor actor, @RequestBody Map<String, Object> form) {
        access.lockMutation();
        prepareNotice(form, projectId, actor);
        form.put("noticeId", noticeId);
        taskService.updateTaskNotice(form);
        return Map.of("success", true, "notice", taskService.getTaskNoticeDetail(noticeId, false));
    }

    @Transactional
    @DeleteMapping("/notices/{noticeId}")
    public Map<String, Object> deleteTaskNotice(@PathVariable("projectId") long projectId, @PathVariable("noticeId") long noticeId) {
        access.lockMutation();
        taskService.deleteTaskNotice(projectId, noticeId);
        return Map.of("success", true);
    }

    // 13. 보고서 CRUD
    
    @Transactional
    @PostMapping("/{taskId}/reports")
    public void createTeamReport(
            @PathVariable("taskId") Long taskId, @PathVariable("projectId") Long projectId,
            @RequestBody TeamReportDto dto) {
        access.lockMutation();
        dto.setTaskId(taskId);
        dto.setProjectId(projectId);
        taskService.createTeamReport(dto);
    }
}
