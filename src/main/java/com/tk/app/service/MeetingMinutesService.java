package com.tk.app.service;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.mapper.MeetingMinutesMapper;
import com.tk.app.repository.MeetingMinuteListView;
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
	@Transactional(rollbackFor = Exception.class)
	public List<MeetingMinutesDto> findMeetingMinutes(
			long workspaceId, long memberID, String kwd, int offset, int size) {
		
		List<MeetingMinuteListView> views = meetingMinuteRepository.findMeetingMinutes(
				workspaceId, memberID, kwd, offset, size);
		
		// 반환할 dto 목록 만들기
		List<MeetingMinutesDto> meetings = new ArrayList<>(views.size());
		// 조회 결과를 꺼내서 view 로 사용
		for(MeetingMinuteListView view : views) {
			// 한건의 정보를 담을 dto 변수
			MeetingMinutesDto meeting = new MeetingMinutesDto();
			// 회의록 번호 dto 에 담기
			meeting.setMeetingMinuteId(view.getMeetingMinuteId());
			meeting.setTitle(view.getTitle());
			meeting.setMeetingDate(view.getMeetingDate());
			meeting.setCreatedDate(view.getCreatedDate());
			meeting.setAuthorName(view.getAuthorName());
			
			try {
				// 본문
				Clob summary = view.getSummary();
				// 본문 값이 있을 때
				if(summary != null) {
					// 읽을 문자열을 dto 목록 본문에 담기
					meeting.setSummary(
							// Math.toIntExact : 첫번째 문자부터 지정한 문자 수 만큼 읽음. 문자수를 int 로 변환
							summary.getSubString(1, Math.toIntExact(summary.length())));
				}
				
			} catch (Exception e) {
				throw new DataRetrievalFailureException("회의록 본문을 읽을 수 없습니다.", e);
			}
			// 완성된 dto 한 건 목록에 추가
			meetings.add(meeting);
			
		}
		
		return meetings;
	}
	
	// 회의록 페이징 처리
	public int countMeetingMinutes(
			long workspaceId, long memberID, String kwd) {
		
		return meetingMinuteRepository.countMeetingMinutes(workspaceId, memberID, kwd);
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
	@Transactional(rollbackFor = Exception.class)
	public int meetingsDelete(
			long workspaceId, long memberId, long meetingMinuteId) {
		
		return meetingMinuteRepository.meetingsDelete(workspaceId, meetingMinuteId, memberId);
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
