package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.GrowthLevelDto;

@Mapper
public interface GrowthTreeMapper {

	public List<GrowthLevelDto> findGrowthLevels();
}
