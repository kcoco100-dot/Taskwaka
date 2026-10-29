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
	private Long dailyReportId; // 일일 보고서 번호
	private Long teamReportId; // 업무 보고서 번호
	private Long projectReportId; // 프로젝트 보고서 번호
	
	private String memberName; // 참여자 이름
	private String actionName; // 활동명
	private String earnedAt; // 지급 일시
	private String revokedAt; // 회수 일시
	private Long projectId; // 프로젝트 번호
	private String projectName; // 활동과 연결된 프로젝트명
}
