package com.tk.app.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.GanttDataDto;
import com.tk.app.domain.dto.HomeDashboardDataDto;
import com.tk.app.domain.dto.MyTaskDto;
import com.tk.app.mapper.MyTaskMapper;

@Service
public class MyTaskService {
	
	private final MyTaskMapper myTaskMapper;
	
	public MyTaskService(MyTaskMapper myTaskMapper) {
		this.myTaskMapper = myTaskMapper;
	}
	
	// 내 업무 목록 조회
	public List<MyTaskDto> findTasksList(long workspaceId, long memberId) {
		
		return myTaskMapper.findTasksList(workspaceId, memberId);
	}
	
	// 간트 조회 
	public GanttDataDto calculateGanttData(List<MyTaskDto> myTasks) {
		GanttDataDto ganttData = new GanttDataDto();
		
		List<LocalDate> ganttDate = new ArrayList<>();
		ganttData.setGanttDate(ganttDate);
		
		// 업무가 있을 때만 실행
					if (!myTasks.isEmpty()) {
						// 첫 업무 시작과 종료 가져오기
						// LocalDate.parse : "2026-10-11" 처럼 변환해주기
						LocalDate ganttStart = LocalDate.parse(myTasks.get(0).getStartAt());
						LocalDate ganttEnd = LocalDate.parse(myTasks.get(0).getEndAt());

						// 업무를 하나씩 꺼내서 반복해주기
						for (MyTaskDto task : myTasks) {
							// 업무의 시작일과 종료일 변환 해주기
							LocalDate start = LocalDate.parse(task.getStartAt());
							LocalDate end = LocalDate.parse(task.getEndAt());

							// 기준이 되는 업무일보다 빠른지 늦은지를 비교해줘야 차트에 다 표시가 가능하다
							// 업무의 시작일이 지금까지 찾은 시작일보다 빠른지 확인
							if (start.isBefore(ganttStart)) {
								// 더 빠르다면 간트의 시작일을 그 날짜로 바꿔주기
								ganttStart = start;
							}

							// 업무의 종료일이 지금까지 찾은 종료일보다 늦은지 확인
							if (end.isAfter(ganttEnd)) {
								// 더 늦다면 간트의 종료일을 그 날짜로 바꿔주기
								ganttEnd = end;
							}
						}

						// 날짜 목록을 채울때 사용할 변수
						LocalDate date = ganttStart;

						// 종료일을 표함하여 날짜 목록 생성하기
						while (!date.isAfter(ganttEnd)) {
							ganttDate.add(date);

							date = date.plusDays(1);
						}

						ganttData.setGanttStart(ganttStart);
						ganttData.setGanttEnd(ganttEnd);
					}
		
		return ganttData;
	}
	// 대시보드
	public HomeDashboardDataDto calculateHomeDashboardData(List<MyTaskDto> myTasks) {
		HomeDashboardDataDto dashboardData = new HomeDashboardDataDto();
		
		// 오늘 할일 집계 로직
		int view = 0; // 할일
		int working = 0; // 진행중
		int preview = 0; // 검토중
		
		// 업무 가져오기
		for(MyTaskDto task : myTasks) {
			if(task.getStatus() != null) {
				if(task.getStatus() == 0) {
					view++;
				} else if(task.getStatus() == 1) {
					working++;
				} else if(task.getStatus() == 2) {
					preview++;
				}
			}  
		}
		
		// 업무 총 건수 계산
		int remainingCount = view + working + preview;
		
		dashboardData.setView(view);
		dashboardData.setWorking(working);
		dashboardData.setPreview(preview);
		dashboardData.setRemainingCount(remainingCount);
		
		LocalDate today = LocalDate.now();
		
		// 오늘이 속한 주의 일요일 구하기
		// 주를 구할 변수 선언
		List<LocalDate> weekDates = new ArrayList<>();
		
		// 일요일 날짜를 담을 변수 선언
		// 일요일의 요일 값 7을 0으로 바꿔 오늘이 일요일이면 오늘부터 시작
		LocalDate sunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
				
		// 실제 일요일 구하는 로직
		for(int i = 0; i < 7; i++) {
			weekDates.add(sunday.plusDays(i));
		}
		
		dashboardData.setToday(today);
		dashboardData.setWeekDates(weekDates);
		
		// 오늘 업무를 담을 목록 만들기
		// 업무 정보 가져오기
		List<MyTaskDto> todayTasks = new ArrayList<>();
		
		// 완료 업무
		int monthlyCompletedCount = 0;
		
		for(MyTaskDto task : myTasks) {
			// 시작일
			LocalDate start = LocalDate.parse(task.getStartAt());
			// 종료일
			LocalDate end = LocalDate.parse(task.getEndAt());
			
			// 시작일 <= 오늘 <= 종료일인 업무만
			if(!today.isBefore(start) && !today.isAfter(end)) {
				todayTasks.add(task);
			}
			
			if(task.getStatus() != null && task.getStatus() == 3
					&& end.getYear() == today.getYear()
					&& end.getMonthValue() == today.getMonthValue()) {
				monthlyCompletedCount++;
			}
		}
		
		dashboardData.setTodayTasks(todayTasks);
		dashboardData.setMonthlyCompletedCount(monthlyCompletedCount);
		
		return dashboardData;
	}
}
