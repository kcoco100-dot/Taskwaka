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
}
