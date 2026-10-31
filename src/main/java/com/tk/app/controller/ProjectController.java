package com.tk.app.controller;

import com.tk.app.domain.dto.ProjectActor;
import com.tk.app.mapper.TaskMapper;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.ProjectDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.domain.dto.SubTaskDto;
import com.tk.app.security.CustomUserDetails;
import com.tk.app.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ProjectAccessAdvice access;
    private final TaskMapper taskMapper;

    @GetMapping("")
    public String projectDetail(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @RequestParam(name = "tab", defaultValue = "dashboard") String tab,
            @RequestParam(name="page", defaultValue = "1") int page,
            @RequestParam(name="kwd", defaultValue = "") String kwd,
            @RequestParam(name="schType", defaultValue = "all") String schType,
            @RequestAttribute("projectActor") ProjectActor actor,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        
    	SessionInfo currentProjectMember = userDetails.getMember();
    	
    	Long currentProjectMemberId = currentProjectMember.getMember_id();
    	String currentProjectMemberName = currentProjectMember.getName();
    	
    	log.info("currentProjectMemberId: " + currentProjectMemberId);
    	
        if (!tab.matches("dashboard|task|gantt|calendar|notice")) {
            tab = "dashboard";
        }
        
        boolean isTeamLeader = actor.isLeader();
        ProjectDto pDto = taskService.getProjectById(projectId);
        if (pDto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        
        model.addAttribute("isTeamLeader", isTeamLeader);
        model.addAttribute("domainKey", domainKey);
        model.addAttribute("projectId", projectId);
        model.addAttribute("activeTab", tab);
        model.addAttribute("projectName", pDto.getSubject());
        model.addAttribute("currentProjectMemberId", currentProjectMemberId);
        model.addAttribute("currentProjectMemberName", currentProjectMemberName);
        if (tab.equals("task") || tab.equals("gantt")) {
            model.addAttribute("tasks", taskService.getTaskList(projectId));
        }
        
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
	        case "notice" -> {
                model.addAttribute("tasks", taskMapper.selectTasksByProjectId(projectId));
	        	int pageSize = 10;
	        	int currentPage = (page < 1) ? 1 : page;
	        	int offset = (currentPage - 1) * pageSize;
	        	if(offset < 0) offset = 0;
	        	
	        	Map<String, Object> map = new HashMap<>();
	        	map.put("schType", schType);
	        	map.put("kwd", kwd);
	        	
	        	map.put("projectId", projectId);
	        	map.put("isTeamLeader", isTeamLeader);

	        	int dataCount = taskService.getTaskNoticeCount(map);
	        	
	        	map.put("offset", offset);
				map.put("pageSize", pageSize);
				
	        	List<Map<String, Object>> taskNotices = taskService.getTaskNoticeList(map);
	        	
	        	model.addAttribute("dataCount", dataCount);
	        	model.addAttribute("taskNotices",taskNotices);
	        	model.addAttribute("schType", schType);
	        	model.addAttribute("page", currentPage);
	        	model.addAttribute("kwd", kwd);
	        }
        }
        return "project/project_layout";
    }

    /**
     * 하위 업무 생성 후 Thymeleaf 프래그먼트 HTML 조각 반환
     */
    @Transactional
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
            @RequestAttribute("projectActor") ProjectActor actor,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {
        access.lockMutation();

    	SessionInfo currentProjectMember = userDetails.getMember();
    	
    	Long currentProjectMemberId = currentProjectMember.getMember_id();
    	
        if (!actor.isLeader() || taskMapper.activeAssignee(projectId, projectMemberId) != 1)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
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
        model.addAttribute("projectMemberId", projectMemberId);
        model.addAttribute("subTask", created);
        model.addAttribute("isTeamLeader", actor.isLeader());
        model.addAttribute("currentProjectMemberId", currentProjectMemberId);

        return "project/fragments/task_items :: subTaskItem";
    }
}