package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HomePersonalScheduleDto {
	private Long scheduleId; // 일정번호
	private Long workspaceId; // 현재 워크번호
	private Long workspaceMemberId; // 개인 일정 소유자 워크 회원번호
	private String title; // 제목
	private String content; // 내용
	private String startAt; // 시작날짜
	private String endAt; // 종료날짜

}
