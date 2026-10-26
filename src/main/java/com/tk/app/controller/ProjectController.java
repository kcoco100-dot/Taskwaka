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
    	String currentProjectMemberName = currentProjectMember.getName();
    	
    	log.info("currentProjectMemberId: " + currentProjectMemberId);
    	
        if (!tab.matches("dashboard|task|gantt|calendar|document|notice")) {
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
        model.addAttribute("currentProjectMemberName", currentProjectMemberName);
        model.addAttribute("tasks", taskService.getTaskList(projectId));
        
        switch(tab) {
	        case "dashboard" -> {
	            model.addAttribute("summary", taskService.getDashboardSummary(projectId));
	            model.addAttribute("upcomingTasks", taskService.getUpcomingSubTasks(projectId));
	            model.addAttribute("notices", taskService.getRecentProjectNotices(projectId));

	        }
	        case "task" -> {
	        	
	        }
	        case "gantt" -> {
	        	
	        }
	        case "calendar" -> {
	        	
	        }
	        case "document" -> {
	        	//model.addAttribute();
	        }
	        case "notice" -> {
	        	model.addAttribute("taskNotices", taskService.getTaskNoticeList(projectId, isTeamLeader));
	        }
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

    	SessionInfo currentProjectMember = userDetails.getMember();
    	
    	Long currentProjectMemberId = currentProjectMember.getMember_id();
    	
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