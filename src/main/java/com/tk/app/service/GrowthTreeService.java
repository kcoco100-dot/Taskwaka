package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;
import com.tk.app.mapper.GrowthTreeMapper;

@Service
public class GrowthTreeService {
	
	private final GrowthTreeMapper growthTreeMapper;
	
	public GrowthTreeService(GrowthTreeMapper growthTreeMapper) {
		this.growthTreeMapper = growthTreeMapper;
	}
	
	// 성장나무 목록 조회
	public List<GrowthLevelDto> findGrowthLevels() {
		
		return growthTreeMapper.findGrowthLevels();
		
	}
	
	// 성장 상태 조회 메서드
	public GrowthTreeDto findGrowthTree(long workspaceId) {
		
		// 성장나무가 없는 워크에서 기본값 처리
		GrowthTreeDto growthTree = growthTreeMapper.findGrowthTree(workspaceId);
		if(growthTree == null) {
			// 새 DTO 만들기
			growthTree = new GrowthTreeDto();
			growthTree.setWorkspaceId(workspaceId);
			growthTree.setTotalExp(0);
		
		
		// 단계 목록 조회
		List<GrowthLevelDto> growthList = findGrowthLevels();
		// 0 EXP 가 포함되는 단계 찾기
		for(GrowthLevelDto growthLists : growthList) {
			if(growthLists.getMinExp() <= 0 
					&& (growthLists.getMaxExp() == null || growthLists.getMaxExp() >= 0)) {
				growthTree.setGrowthLevelId(growthLists.getGrowthLevelId());
				growthTree.setLevelNo(growthLists.getLevelNo());
				growthTree.setLevelName(growthLists.getLevelName());
				growthTree.setTreeImagePath(growthLists.getTreeImagePath());
				
				break;
			}
		}		
	}
		
		return growthTree;
	}
}
