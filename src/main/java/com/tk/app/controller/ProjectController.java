package com.tk.app.controller;

import com.tk.app.domain.dto.ProjectDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.security.CustomUserDetails;
import com.tk.app.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
    	SessionInfo currentProjectMember = userDetails.getMember();
    	
    	Long currentProjectMemberId = currentProjectMember.getMember_id();
    	
    	log.info("currentProjectMemberId: " + currentProjectMemberId);
    	
     // 탭 정규식 허용 범위에 notice 추가
        if (!tab.matches("notice|dashboard|task|gantt|calendar|file|document")) {
            tab = "dashboard";
        }

        boolean isTeamLeader = taskService.isTeamLeaderOrAbove(currentProjectMemberId);
        ProjectDto pDto = taskService.getProjectById(projectId);
        
        model.addAttribute("isTeamLeader", isTeamLeader);
        model.addAttribute("domainKey", domainKey);
        model.addAttribute("projectId", projectId);
        model.addAttribute("activeTab", tab);
        model.addAttribute("projectName", pDto.getSubject());
        model.addAttribute("currentProjectMemberId", currentProjectMemberId);
        
        if ("dashboard".equals(tab)) {
            model.addAttribute("summary", taskService.getDashboardSummary(projectId));
            model.addAttribute("upcomingTasks", taskService.getUpcomingSubTasks(projectId));
            model.addAttribute("notices", taskService.getRecentProjectNotices(projectId));
            model.addAttribute("tasks", taskService.getTaskList(projectId));
        } else if ("task".equals(tab) || "gantt".equals(tab) || "calendar".equals(tab)) {
            model.addAttribute("tasks", taskService.getTaskList(projectId));
        }else if ("notice".equals(tab)) {
            model.addAttribute("taskNotices", taskService.getTaskNoticeList(projectId, isTeamLeader));
            model.addAttribute("tasks", taskService.getTaskList(projectId));
        }

        return "project/project_layout";
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
            @RequestParam(value = "currentProjectMemberId", defaultValue = "1") Long currentProjectMemberId,
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
        model.addAttribute("currentProjectMemberId", currentProjectMemberId);

        return "project/fragments/task_items :: subTaskItem";
    }
}