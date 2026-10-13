package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.MeetingMinutesDto;

@Mapper
public interface MeetingMinutesMapper {
	
	// 업무 회의록 목록 조회
	public List<MeetingMinutesDto> findMeetingMinutes(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("kwd") String kwd,
			@Param("offset") int offset,
			@Param("size") int size);
	
	// 회의록 목록 페이징처리
	public int countMeetingMinutes(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("kwd") String kwd);
	
	// 회의록 상세 조회
	public MeetingMinutesDto findMeetingDetailMinutes(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("meetingMinuteId") long meetingMinuteId);
	
	// 회의록 제목 및 내용 수정
	public int meetingsEdit(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("meetingMinuteId") long meetingMinuteId,
			@Param("title") String title,
			@Param("content") String content);
	
	// 회의록 삭제
	public int meetingsDelete(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId,
			@Param("meetingMinuteId") long meetingMinuteId);
}
