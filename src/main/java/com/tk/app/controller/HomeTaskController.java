package com.tk.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.domain.dto.GanttDataDto;
import com.tk.app.domain.dto.MyTaskDto;
import com.tk.app.service.MyTaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class HomeTaskController {
	
	private final MyTaskService myTaskService;

	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}
	

	// 담당 업무 기간 및 진행률 Home 캘린더에 표시
	@GetMapping("/calendar")
	public String calendar(
			@RequestAttribute("workspace") WorkspaceDto workspace, 
			Model model) {

		List<MyTaskDto> myTasks = myTaskService.findTasksList(
				workspace.getWorkspaceId(), workspace.getMemberId());

		model.addAttribute("myTasks", myTasks);

		return "main/home";
	}
	
	// 담당 업무 기간 및 진행률 gantt 에 표시
		@GetMapping("/gantt")
		public String gantt(
				@RequestAttribute("workspace") WorkspaceDto workspace, 
				Model model) {

			List<MyTaskDto> myTasks = myTaskService.findTasksList(workspace.getWorkspaceId(), workspace.getMemberId());
			
			GanttDataDto ganttData = myTaskService.calculateGanttData(myTasks);
			// 업무가 있을 때만 실행
			if (!myTasks.isEmpty()) {
				
				model.addAttribute("ganttStart", ganttData.getGanttStart());
				model.addAttribute("ganttEnd", ganttData.getGanttEnd());
			}

			model.addAttribute("myTasks", myTasks);
			model.addAttribute("ganttDate", ganttData.getGanttDate());

			return "home/gantt";
		}
}
