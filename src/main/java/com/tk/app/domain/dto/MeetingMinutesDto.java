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
	private String createdDate; // 작성일(최초 등록일)
	private String authorName; // 작성자 이름
	
	private int canModify; // 수정, 삭제 가능 여부 1=가능, 0=불가능
	
	// 회의록 등록에서 필요한 컬럼
	private Long workspaceId; // 현재 워크 번호
	private Long projectId; // 등록할 프로젝트 번호
	private Long subTaskId; // 하위업무 번호
	private Long memberId; // 로그인 회원 번호
}
