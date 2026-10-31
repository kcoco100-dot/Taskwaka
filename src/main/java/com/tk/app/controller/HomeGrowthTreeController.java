package com.tk.app.controller;

import com.tk.app.service.TreeShopService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private final TreeShopService shop;

    @GetMapping("/growthTree/shop")
    @ResponseBody
    public TreeShopService.State shopState(@RequestAttribute("workspace") WorkspaceDto workspace) {
        return shop.state(workspace.getWorkspaceId());
    }

    @PutMapping("/growthTree/shop/{itemId}")
    @ResponseBody
    public TreeShopService.State equip(@RequestAttribute("workspace") WorkspaceDto workspace,
            @PathVariable("itemId") long itemId, @RequestParam("equipped") boolean equipped) {
        return shop.setEquipped(workspace.getWorkspaceId(), workspace.getMemberId(), itemId, equipped);
    }
	
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
		
		// 다음 단계까지 00 EXP와 최고 단계 달성 표시
		GrowthLevelDto nextGrowthLevel = null;
		// 단계 번호순으로 조회된 목록 확인
		for(GrowthLevelDto level : growthLevels) {
			// 확인 중인 단계가 현재 나무 단계보다 높은지
			if(level.getLevelNo() > growthTree.getLevelNo()) {
				// 처음 찾은 높은 단계를 다음 단계로 저장
				nextGrowthLevel = level;
				break;
			}
		}
		
		// 남은 경험치를 담을 변수
		long remainingGrowthExp = 0L;
		// 다음 단계가 있을때
		if(nextGrowthLevel != null) {
			// 최소 경험치 - 현재 경험치
			remainingGrowthExp = Math.max(
					0L, nextGrowthLevel.getMinExp() - growthTree.getTotalExp());
		}
		
		model.addAttribute("growthLevels", growthLevels);
		model.addAttribute("remainingGrowthExp", remainingGrowthExp);
		model.addAttribute("nextGrowthLevel", nextGrowthLevel);
		model.addAttribute("growthTree", growthTree);
		model.addAttribute("recentGrowthExpLogs", 
				growthTreeService.findRecentGrowthExpLogs(workspace.getWorkspaceId()));
		
		return "tree/growthTree";
	}

}
