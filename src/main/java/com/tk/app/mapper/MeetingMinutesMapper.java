package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.MeetingMinutesDto;

@Mapper
public interface MeetingMinutesMapper {
	
	// 회의록 등록
	public int insertMeetingMinutes(MeetingMinutesDto meeting);
	
	// 업무 회의록 목록 조회
	public List<MeetingMinutesDto> findMeetingMinutes(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("kwd") String kwd,
			@Param("offset") int offset,
			@Param("size") int size);
	
	// 회의록 상세 조회
	public MeetingMinutesDto findMeetingDetailMinutes(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("meetingMinuteId") long meetingMinuteId);
	
	// 회의록 이전글
	public MeetingMinutesDto findByPrevMeeting(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("meetingMinuteId") long meetingMinuteId);
	
	// 회의록 다음글
	public MeetingMinutesDto findByNextMeeting(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("meetingMinuteId") long meetingMinuteId);

}
