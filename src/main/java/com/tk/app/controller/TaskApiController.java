package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.domain.dto.TaskMemberDto;
import com.tk.app.domain.dto.TeamReportDto;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.GrowthTreeService;
import com.tk.app.service.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/{domainKey}/project/{projectId}/api/tasks")
@RequiredArgsConstructor
public class TaskApiController {

    private final TaskService taskService;
    private final GrowthTreeService growthTreeService;


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

    // 5. 메인 업무 실시간 필드 수정
//    @PatchMapping("/{taskId}/field")
//    public ResponseEntity<Map<String, Object>> updateTaskField(
//            @PathVariable("taskId") Long taskId,
//            @RequestBody Map<String, Object> req) {
//        String fieldName = (String) req.get("fieldName");
//        Object fieldValue = req.get("fieldValue");
//        taskService.updateTaskField(taskId, fieldName, fieldValue);
//
//        Map<String, Object> res = new HashMap<>();
//        res.put("success", true);
//        return ResponseEntity.ok(res);
//    }

    // 6. 메인 업무 상세 내용 저장
    @PutMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> updateTaskDetail(
            @PathVariable("taskId") Long taskId,
            @RequestBody TaskDto dto) {
        dto.setTaskId(taskId);
        taskService.updateTaskDetail(dto);
        //storageService.uploadFileToServer(null, uploadPath);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }
    
//    @PutMapping("/{taskId}/files")
//    public ResponseEntity<>uploadTaskFiles(
//    		@PathVariable("taskId") Long taskId,
//    		
//    		){
//    	
//    }
//    @PutMapping("/subtasks/{subTaskId}/files")
//    public ResponseEntity<>uploadSubTaskFiles(
//    		@PathVariable("subTaskId") Long taskId,
//    		
//    		){
//    	
//    }

//    // 7. 하위 업무 수정 (우측 패널에서 호출)
//    @PostMapping("/subtasks/{subTaskId}")
//    public ResponseEntity<Map<String, Object>> updateSubTaskWithFiles(
//            @PathVariable("projectId") Long projectId,
//            @PathVariable("subTaskId") Long subTaskId,
//            @ModelAttribute SubTaskDto dto) { // @RequestBody가 아니라 @ModelAttribute 사용!
//
//        Map<String, Object> res = new HashMap<>();
//        try {
//            dto.setSubTaskId(subTaskId);
//
//            // 1. 하위업무 텍스트 정보 업데이트 + 첨부파일 저장 (Service에서 일괄 처리)
//            taskService.updateTaskDetail(dto);
//
//            // 2. 저장 후 갱신된 첨부파일 목록 조회해서 내려주기 (선택)
//            // res.put("files", taskService.getSubTaskFiles(subTaskId));
//            res.put("success", true);
//            return ResponseEntity.ok(res);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            res.put("success", false);
//            res.put("message", "저장 중 오류가 발생했습니다.");
//            return ResponseEntity.status(500).body(res);
//        }
//    }

 // 8. 하위 업무 상세 내용 및 첨부파일 저장
    @PutMapping("/subtasks/{subTaskId}")
    public ResponseEntity<Map<String, Object>> updateSubTaskDetail(
            @PathVariable("subTaskId") Long subTaskId,
            @ModelAttribute SubTaskDto dto) { 

        dto.setSubTaskId(subTaskId);
        
        taskService.updateSubTaskDetail(dto);
        //res.put("files", taskService.getSubTaskFiles(subTaskId));

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    // 9. 하위 업무 삭제
    @DeleteMapping("/subtasks/{subTaskId}")
    public ResponseEntity<Map<String, Object>> deleteSubTask(@PathVariable("subTaskId") Long subTaskId) {
        taskService.deleteSubTask(subTaskId);
        //storageService.deleteFile(uploadPath, null);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }
    
    // 다운로드 링크 필요

    // 10. 일일 보고서 (daily_reports) CRUD API
    @GetMapping("/subtasks/{subTaskId}/daily-reports")
    public ResponseEntity<List<DailyReportDto>> getDailyReports(@PathVariable("subTaskId") Long subTaskId) {
        return ResponseEntity.ok(taskService.getDailyReports(subTaskId));
    }

    @PostMapping("/subtasks/{subTaskId}/daily-reports")
    public ResponseEntity<DailyReportDto> createDailyReport(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("subTaskId") Long subTaskId,
            @RequestBody DailyReportDto dto) {

        dto.setSubTaskId(subTaskId);

        return ResponseEntity.ok(
                growthTreeService.createDailyReportWithExp(dto, projectId, domainKey));
    }

    @PutMapping("/daily-reports/{dailyReportId}")
    public ResponseEntity<Map<String, Object>> updateDailyReport(
            @PathVariable("dailyReportId") Long dailyReportId,
            @RequestBody DailyReportDto dto) {
        dto.setDailyReportId(dailyReportId);
        boolean updated = taskService.updateDailyReport(dto);
        Map<String, Object> res = new HashMap<>();
        res.put("success", updated);
        if (!updated) {
            res.put("message", "상급자가 이미 열람/확인한 보고서는 수정할 수 없습니다.");
        }
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/daily-reports/{dailyReportId}")
    public ResponseEntity<Map<String, Object>> deleteDailyReport(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("dailyReportId") Long dailyReportId) {

        Map<String, Object> res = new HashMap<>();

        try {
            boolean deleted = growthTreeService.deleteDailyReportWithExp(
                    dailyReportId, projectId, domainKey);

            res.put("success", deleted);

            if (!deleted) {
                res.put("message", "이미 삭제되었거나 삭제할 수 없는 보고서입니다.");
                return ResponseEntity.status(409).body(res);
            }

            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            res.put("success", false);
            res.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (RuntimeException e) {
            res.put("success", false);
            res.put("message", "일일 보고서 삭제 처리 중 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(res);
        }
    }
    
    // 11. 캘린더 일정 (업무 연동 + 개인 일정 추가/삭제) API 
    @GetMapping("/calendar-events")
    public ResponseEntity<List<com.tk.app.domain.dto.CalendarEventDto>> getCalendarEvents(
            @PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(taskService.getCalendarEvents(projectId));
    }

    @PostMapping("/calendar-events")
    public ResponseEntity<com.tk.app.domain.dto.CalendarEventDto> createCalendarEvent(
            @PathVariable("projectId") Long projectId,
            @RequestBody com.tk.app.domain.dto.CalendarEventDto dto) {
        dto.setProjectId(projectId);
        return ResponseEntity.ok(taskService.createPersonalEvent(dto));
    }

    @DeleteMapping("/calendar-events/{scheduleId}")
    public ResponseEntity<Map<String, Object>> deleteCalendarEvent(
            @PathVariable("scheduleId") Long scheduleId) {
        boolean deleted = taskService.deletePersonalEvent(scheduleId);
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

    @PostMapping("/notices")
    public ResponseEntity<Map<String, Object>> createTaskNotice(
            @PathVariable("projectId") Long projectId,
            @RequestBody Map<String, Object> req) {

        Long currentPmId = Long.valueOf(String.valueOf(req.get("currentProjectMemberId")));

        Map<String, Object> res = new HashMap<>();
        Map<String, Object> isTeamLeaderMap = new HashMap<>();
        isTeamLeaderMap.put("currentProjectMemberId", currentPmId);
        isTeamLeaderMap.put("projectId", projectId);
        
        if (!taskService.isTeamLeaderOrAbove(isTeamLeaderMap)) {
            res.put("success", false);
            res.put("message", "팀 리더만 공지를 작성할 수 있습니다.");
            return ResponseEntity.status(403).body(res);
        }

        req.put("projectId", projectId);
        req.put("memberId", currentPmId);
        if (req.get("taskId") != null && String.valueOf(req.get("taskId")).isBlank()) {
            req.put("taskId", null);
        }

        Map<String, Object> created = taskService.createTaskNotice(req);
        res.put("success", true);
        res.put("notice", created);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/notices/{noticeId}")
    public ResponseEntity<Map<String, Object>> updateTaskNotice(
    		@PathVariable("projectId")Long projectId,
            @PathVariable("noticeId") Long noticeId,
            @RequestBody Map<String, Object> req) {

        Long currentPmId = Long.valueOf(String.valueOf(req.get("currentProjectMemberId")));

        Map<String, Object> res = new HashMap<>();
        Map<String, Object> isTeamLeaderMap = new HashMap<>();
        isTeamLeaderMap.put("currentProjectMemberId", currentPmId);
        isTeamLeaderMap.put("projectId", projectId);
        if (!taskService.isTeamLeaderOrAbove(isTeamLeaderMap)) {
            res.put("success", false);
            res.put("message", "팀 리더만 공지를 수정할 수 있습니다.");
            return ResponseEntity.status(403).body(res);
        }

        req.put("noticeId", noticeId);
        if (req.get("taskId") != null && String.valueOf(req.get("taskId")).isBlank()) {
            req.put("taskId", null);
        }

        taskService.updateTaskNotice(req);
        res.put("success", true);
        res.put("notice", taskService.getTaskNoticeDetail(noticeId, false));
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<Map<String, Object>> deleteTaskNotice(
    		@PathVariable("projectId") Long projectId,
            @PathVariable("noticeId") Long noticeId,
            @RequestParam(value = "currentProjectMemberId") Long currentPmId) {

        Map<String, Object> res = new HashMap<>();
        Map<String, Object> isTeamLeaderMap = new HashMap<>();
        isTeamLeaderMap.put("currentProjectMemberId", currentPmId);
        isTeamLeaderMap.put("projectId", projectId);
        if (!taskService.isTeamLeaderOrAbove(isTeamLeaderMap)) {
            res.put("success", false);
            res.put("message", "팀 리더만 공지를 삭제할 수 있습니다.");
            return ResponseEntity.status(403).body(res);
        }

        taskService.deleteTaskNotice(noticeId);
        res.put("success", true);
        return ResponseEntity.ok(res);
    }
    
    // 13. 보고서 CRUD
    
    @PostMapping("/{taskId}/reports")
    public void createTaskNotice(
            @PathVariable("taskId") Long taskId,
            @RequestBody TeamReportDto dto) {
    	taskService.createTeamReport(dto);
    }
}