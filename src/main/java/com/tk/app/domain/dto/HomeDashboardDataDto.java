package com.tk.app.domain.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HomeDashboardDataDto {
	private int view; // 할일 
	private int working; // 진행중
	private int preview; // 검토중 건수
	private int remainingCount; // 할일 + 진행중 + 검토중 합계
	private LocalDate today; // 오늘 날짜
	private List<LocalDate> weekDates; // 이번주 월~일까지
	private List<MyTaskDto> todayTasks; // 오늘이 업무 기간에 포함되는 담당 업무 목록
	private int monthlyCompletedCount; // 이번달 종료 예정 + 현재 완료 상태 업무 건수
	
}