package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GrowthExpLogDto {
	private long expLogId; // 지급 이력 번호
	private long workspaceId; // 경험치를 받을 워크번호
	private long memberId; // 활동한 회원번호
	private Long noticeId; // 지급 공지번호
	private long exp; // 지급 경험치
	private String actionType; // 지급 사유
	
	private String inviteKey; // DB에 저장할 초대 식별값
	private Long subTaskId; // 하위업무 번호를 담을 변수
	private Long dailyReportId; // 일일보고서 번호
}
