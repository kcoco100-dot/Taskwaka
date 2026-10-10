package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.MeetingMinutesDto;

@Mapper
public interface MeetingMinutesMapper {
	
	// 업무 회의록 목록 조회
	List<MeetingMinutesDto> findMeetingMinutes(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId);
}
