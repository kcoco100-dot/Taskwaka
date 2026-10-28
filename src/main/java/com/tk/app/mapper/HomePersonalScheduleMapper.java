package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.HomePersonalScheduleDto;

@Mapper
public interface HomePersonalScheduleMapper {
	// 소유자 확인
	Long findActiveWorkspaceMemberId(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId);
	
	// 개인 일정 캘린더에 등록
	int insertPersonalSchedule(HomePersonalScheduleDto dto);
	
	// 개인 일정 목록
	List<HomePersonalScheduleDto> findPersonalSchedules(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId);
}