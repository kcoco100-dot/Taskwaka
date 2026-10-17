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
import com.tk.app.domain.dto.ParticipatingProjectDto;
import com.tk.app.service.ParticipatingProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class HomeProjectController {
	
	private final ParticipatingProjectService participatingProjectService;

	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}

	// projects 페이지
	@GetMapping("/projects")
	public String projects(
			@RequestAttribute("workspace") WorkspaceDto workspace, 
			Model model) {

		// 프로젝트 목록 조회
		List<ParticipatingProjectDto> participatingProjects = participatingProjectService
				.findParticipatingProjects(workspace.getWorkspaceId(), workspace.getMemberId());

		// 조회한 데이터를 담아서 HTML 에 보낸다.
		model.addAttribute("participatingProjects", participatingProjects);

		return "main/projects";
	}
}
