package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;

@Mapper
public interface GrowthTreeMapper {
	
	// 성장나무 단계 화면
	public List<GrowthLevelDto> findGrowthLevels();
	
	// 성장나무 화면
	public GrowthTreeDto findGrowthTree(long workspaceId);
}
