package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.GrowthLevelDto;
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
}
