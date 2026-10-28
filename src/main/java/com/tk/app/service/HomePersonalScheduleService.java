package com.tk.app.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.common.MyUtil;
import com.tk.app.domain.dto.HomePersonalScheduleDto;
import com.tk.app.mapper.HomePersonalScheduleMapper;

@Service
public class HomePersonalScheduleService {

	private final HomePersonalScheduleMapper homePersonalScheduleMapper;
	private final MyUtil myUtil;
	
	public HomePersonalScheduleService(
			HomePersonalScheduleMapper homePersonalScheduleMapper, 
			MyUtil myUtil) {
		
		this.homePersonalScheduleMapper = homePersonalScheduleMapper;
		this.myUtil = myUtil;
	}
	
	
	// 개인 일정 목록 조회
	public List<HomePersonalScheduleDto> findPersonalSchedules(
			long workspaceId, long memberId) {
		
		return homePersonalScheduleMapper.findPersonalSchedules(workspaceId, memberId);
	}
	
	// 개인 일정 캘린더에 등록
	@Transactional
	public int insertPersonalSchedule(
			long workspaceId, long memberId, HomePersonalScheduleDto dto) {
		
		// 일정 정보가 없을 때
		if(dto == null) {
			throw new IllegalStateException("개인 일정 정보가 없습니다.");
		}
		
		// 필수 입력사항
		if(myUtil.isEmpty(dto.getTitle()) 
				|| myUtil.isEmpty(dto.getStartAt()) 
				|| myUtil.isEmpty(dto.getEndAt())) {
			throw new IllegalStateException("제목, 시작일, 종료일 작성은 필수 입니다.");
		}
		
		// 시작일 형식 검사
		if(!dto.getStartAt().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")
				|| !dto.getEndAt().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
			throw new IllegalStateException("날짜 형식은 '예시 : 2026-10-28' 로 작성해야 합니다.");
		}
		
		try {
			LocalDate startAt = LocalDate.parse(dto.getStartAt());
			LocalDate endAt = LocalDate.parse(dto.getEndAt());
			
			if(startAt.getYear() < 1 || endAt.getYear() < 1) {
				throw new IllegalStateException("연도는 0001 이상이어야 합니다.");
			}
			
			if(endAt.isBefore(startAt)) {
				throw new IllegalStateException("종료일은 시작일보다 빠를 수 없습니다.");
			} 
			
		} catch (DateTimeParseException e) {
			throw new IllegalStateException("존재하지 않는 날짜입니다.", e);
		} 
		
		// 유효한 소유자를 조회하고 DTO에 서버 기준 값을 설정
		Long workspaceMemberId = homePersonalScheduleMapper.findActiveWorkspaceMemberId(workspaceId, memberId);
		if(workspaceMemberId == null) {
			throw new IllegalStateException("현재 워크에 참여 중인 회원만 등록할 수 있습니다.");
		}
		
		dto.setWorkspaceId(workspaceId);
		dto.setWorkspaceMemberId(workspaceMemberId);
		
		// 등록 건수와 생성된 일정 번호 검사
		int insertCount = homePersonalScheduleMapper.insertPersonalSchedule(dto);
		if(insertCount != 1) {
			throw new IllegalStateException("개인 일정을 등록하지 못했습니다.");
		}
		
		// 스케줄번호 확인
		if(dto.getScheduleId() == null) {
			throw new IllegalStateException("등록된 일정 번호를 찾을 수 없습니다.");
		}
		
		return insertCount;
	}
}
