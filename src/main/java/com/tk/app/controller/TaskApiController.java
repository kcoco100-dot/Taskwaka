package com.tk.app.controller;

import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskApiController {

    private final TaskService taskService;

    // 1. 전체 업무 목록 조회
    @GetMapping
    public ResponseEntity<List<TaskDto>> getTaskList(@PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(taskService.getTaskList(projectId));
    }

    // 2. 업무 상세 단건 조회
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTaskDetail(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(taskService.getTaskDetail(taskId));
    }

    // 3. 메인 업무 생성
    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @PathVariable("projectId") Long projectId,
            @RequestBody TaskDto dto) {
        dto.setProjectId(projectId);
        if (dto.getProjectMemberId() == null) {
            dto.setProjectMemberId(1L); // 기본값 매핑
        }
        return ResponseEntity.ok(taskService.createMainTask(dto));
    }

    // 4. 메인 업무 인라인 필드 수정
    @PatchMapping("/{taskId}/field")
    public ResponseEntity<Map<String, Object>> updateTaskField(
            @PathVariable("taskId") Long taskId,
            @RequestBody Map<String, Object> req) {
        String fieldName = (String) req.get("fieldName");
        Object fieldValue = req.get("fieldValue");
        taskService.updateTaskField(taskId, fieldName, fieldValue);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    // 5. 상세창 전체 저장
    @PutMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> updateTaskDetail(
            @PathVariable("taskId") Long taskId,
            @RequestBody TaskDto dto) {
        dto.setTaskId(taskId);
        taskService.updateTaskDetail(dto);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    // 6. 메인 업무 삭제
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable("taskId") Long taskId) {
        taskService.deleteTask(taskId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    // 7. 하위 업무 생성
    @PostMapping("/{taskId}/subtasks")
    public ResponseEntity<SubTaskDto> createSubTask(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @RequestBody SubTaskDto dto) {
        dto.setProjectId(projectId);
        dto.setTaskId(taskId);
        if (dto.getProjectMemberId() == null) {
            dto.setProjectMemberId(1L);
        }
        return ResponseEntity.ok(taskService.createSubTask(dto));
    }

    // 8. 하위 업무 필드 수정
    @PatchMapping("/subtasks/{subTaskId}/field")
    public ResponseEntity<Map<String, Object>> updateSubTaskField(
            @PathVariable("subTaskId") Long subTaskId,
            @RequestBody Map<String, Object> req) {
        String fieldName = (String) req.get("fieldName");
        Object fieldValue = req.get("fieldValue");
        taskService.updateSubTaskField(subTaskId, fieldName, fieldValue);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }

    // 9. 하위 업무 삭제
    @DeleteMapping("/subtasks/{subTaskId}")
    public ResponseEntity<Map<String, Object>> deleteSubTask(@PathVariable("subTaskId") Long subTaskId) {
        taskService.deleteSubTask(subTaskId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return ResponseEntity.ok(res);
    }
}
