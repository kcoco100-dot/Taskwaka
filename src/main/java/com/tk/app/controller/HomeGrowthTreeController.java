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
import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;
import com.tk.app.service.GrowthTreeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class HomeGrowthTreeController {
	
	private final GrowthTreeService growthTreeService;
	
	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}

	// 성장나무 화면
	@GetMapping("/growthTree")
	public String growthTree(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			Model model) {
		
		List<GrowthLevelDto> growthLevels = growthTreeService.findGrowthLevels();
		// 나무 생성
		GrowthTreeDto growthTree = growthTreeService.findGrowthTree(workspace.getWorkspaceId());
		
		model.addAttribute("growthLevels", growthLevels);
		model.addAttribute("growthTree", growthTree);
		model.addAttribute("recentGrowthExpLogs", 
				growthTreeService.findRecentGrowthExpLogs(workspace.getWorkspaceId()));
		
		return "tree/growthTree";
	}

}
