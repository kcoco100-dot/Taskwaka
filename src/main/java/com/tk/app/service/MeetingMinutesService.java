package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.mapper.MeetingMinutesMapper;
import com.tk.app.repository.MeetingMinuteRepository;

@Service
public class MeetingMinutesService {
	
	private final MeetingMinutesMapper meetingMinutesMapper;
	private final MeetingMinuteRepository meetingMinuteRepository;
	
	public MeetingMinutesService(
			MeetingMinutesMapper meetingMinutesMapper, 
			MeetingMinuteRepository meetingMinuteRepository) {
		this.meetingMinutesMapper = meetingMinutesMapper;
		this.meetingMinuteRepository = meetingMinuteRepository;
	}
	
	// 워크 회의록 등록
	public int insertMeetingMinutes(
			long workspaceId, long projectId, long subTaskId, long memberId,
			String title, String content) {
		
		MeetingMinutesDto meeting = new MeetingMinutesDto();
		meeting.setWorkspaceId(workspaceId);
		meeting.setProjectId(projectId);
		meeting.setSubTaskId(subTaskId);
		meeting.setMemberId(memberId);
		meeting.setTitle(title);
		meeting.setContent(content);
		
		return meetingMinutesMapper.insertMeetingMinutes(meeting);
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
	@Transactional(rollbackFor = Exception.class)
	public int meetingsEdit(
			long workspaceId, long memberId, long meetingMinuteId,
			String title, String content) {
		
		return meetingMinuteRepository.meetingsEdit(workspaceId, meetingMinuteId, memberId, title, content);
	}
	
	// 워크 회의록 삭제
	public int meetingsDelete(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinutesMapper.meetingsDelete(workspaceId, memberId, meetingMinuteId);
	}
	
	// 이전글
	public MeetingMinutesDto findByPrevMeeting(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinutesMapper.findByPrevMeeting(workspaceId, memberId, meetingMinuteId);
	}
	
	// 다음글
	public MeetingMinutesDto findByNextMeeting(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinutesMapper.findByNextMeeting(workspaceId, memberId, meetingMinuteId);
	}
}
