package com.tk.app.controller;

import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/project/{projectId}")
public class ProjectController {

    private final TaskService taskService;

    @GetMapping("")
    public String projectDetail(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "tab", defaultValue = "feed") String tab,
            Model model) {
        
        if (!tab.matches("feed|dashboard|task|gantt|calendar|file")) {
            tab = "feed";
        }
        
        model.addAttribute("domainKey", domainKey);
        model.addAttribute("projectId", projectId);
        model.addAttribute("activeTab", tab);
        model.addAttribute("projectName", "Taskwaka 시작가이드");

        // 업무 탭인 경우 초기 화면 렌더링용 업무 목록 주입 (SSR)
        if ("task".equals(tab)) {
            model.addAttribute("tasks", taskService.getTaskList(projectId));
        }
        
        return "project/project_layout";
    }

    /**
     * 메인 업무 생성 후 Thymeleaf 프래그먼트 HTML 조각 반환
     */
    @PostMapping("/tasks/fragment/create-main")
    public String createMainTaskFragment(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("projectMemberId") Long projectMemberId,
            @RequestParam(value = "status", defaultValue = "0") Integer status,
            @RequestParam(value = "startAt", required = false) String startAt,
            @RequestParam(value = "endAt", required = false) String endAt,
            @RequestParam(value = "taskLevel", defaultValue = "1") Integer taskLevel,
            Model model) {

        TaskDto dto = new TaskDto();
        dto.setProjectId(projectId);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setProjectMemberId(projectMemberId);
        dto.setStatus(status);
        dto.setStartAt((startAt != null && !startAt.isBlank()) ? startAt : null);
        dto.setEndAt((endAt != null && !endAt.isBlank()) ? endAt : null);
        dto.setTaskLevel(taskLevel);

        TaskDto created = taskService.createMainTask(dto);
        model.addAttribute("task", created);

        return "project/fragments/task_items :: mainTaskItem";
    }

    /**
     * 하위 업무 생성 후 Thymeleaf 프래그먼트 HTML 조각 반환
     */
    @PostMapping("/tasks/{taskId}/fragment/create-sub")
    public String createSubTaskFragment(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("projectMemberId") Long projectMemberId,
            @RequestParam(value = "status", defaultValue = "0") Integer status,
            @RequestParam(value = "startAt", required = false) String startAt,
            @RequestParam(value = "endAt", required = false) String endAt,
            Model model) {

        SubTaskDto dto = new SubTaskDto();
        dto.setProjectId(projectId);
        dto.setTaskId(taskId);
        dto.setTitle(title);
        dto.setContent(content);
        dto.setProjectMemberId(projectMemberId);
        dto.setStatus(status);
        dto.setStartAt((startAt != null && !startAt.isBlank()) ? startAt : null);
        dto.setEndAt((endAt != null && !endAt.isBlank()) ? endAt : null);

        SubTaskDto created = taskService.createSubTask(dto);
        model.addAttribute("subTask", created);

        return "project/fragments/task_items :: subTaskItem";
    }
}
