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
	
	// 워크 회의록 목록 조회
	public List<MeetingMinutesDto> findMeetingMinutes(long workspaceId, long memberID) {
		
		return meetingMinutesMapper.findMeetingMinutes(workspaceId, memberID);
	}
	
	// 워크 회의록 상세 조회
	public MeetingMinutesDto findMeetingDetailMinutes(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinutesMapper.findMeetingDetailMinutes(workspaceId, memberId, meetingMinuteId);
	}
}
