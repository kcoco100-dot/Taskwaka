package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.mapper.MeetingMinutesMapper;

@Service
public class MeetingMinutesService {
	
	private final MeetingMinutesMapper meetingMinutesMapper;
	
	public MeetingMinutesService(MeetingMinutesMapper meetingMinutesMapper) {
		this.meetingMinutesMapper = meetingMinutesMapper;
	}
	
	// 워크 회의록 목록 조회 및 검색
	public List<MeetingMinutesDto> findMeetingMinutes(
			long workspaceId, long memberID, String kwd, int offset, int size) {
		
		return meetingMinutesMapper.findMeetingMinutes(workspaceId, memberID, kwd, offset, size);
	}
	
	// 회의록 페이징 처리
	public int countMeetingMinutes(
			long workspaceId, long memberID, String kwd) {
		
		return meetingMinutesMapper.countMeetingMinutes(workspaceId, memberID, kwd);
	}
	
	// 워크 회의록 상세 조회
	public MeetingMinutesDto findMeetingDetailMinutes(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinutesMapper.findMeetingDetailMinutes(workspaceId, memberId, meetingMinuteId);
	}
	
	// 워크 회의록 수정
	public int meetingsEdit(
			long workspaceId, long memberId, long meetingMinuteId,
			String title, String content) {
		
		return meetingMinutesMapper.meetingsEdit(workspaceId, memberId, meetingMinuteId, title, content);
	}
	
	// 워크 회의록 삭제
	public int meetingsDelete(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinutesMapper.meetingsDelete(workspaceId, memberId, meetingMinuteId);
	}
}
