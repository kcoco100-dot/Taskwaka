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
            @RequestParam(name = "tab", defaultValue = "dashboard") String tab,
            Model model) {
        
        if (!tab.matches("dashboard|task|gantt|calendar|file")) {
            tab = "dashboard";
        }
        
        
        model.addAttribute("domainKey", domainKey);
        model.addAttribute("projectId", projectId);
        model.addAttribute("activeTab", tab);
        
        String projectName = taskService.getProjectSubject(projectId);
        model.addAttribute("projectName", projectName);

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
            @RequestParam("taskLevel") Integer taskLevel,
            Model model) {

        TaskDto dto = new TaskDto();
        dto.setProjectId(projectId);
        dto.setTitle(title);
        dto.setTaskLevel(taskLevel);
        dto.setProjectMemberId(1L);

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
            Model model) {

        SubTaskDto dto = new SubTaskDto();
        dto.setProjectId(projectId);
        dto.setTaskId(taskId);
        dto.setTitle(title);
        dto.setProjectMemberId(1L);

        SubTaskDto created = taskService.createSubTask(dto);
        model.addAttribute("subTask", created);

        return "project/fragments/task_items :: subTaskItem";
    }
}
