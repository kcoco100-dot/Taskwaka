package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetingMinutesDto {
	private Long meetingMinuteId; // 회의록 번호
	private String title; // 회의 제목
	private String content; // 상세 본문
	
	private String summary; // 목록 표시
	private String meetingDate; // 회의 일자
	private String authorName; // 작성자 이름
}
