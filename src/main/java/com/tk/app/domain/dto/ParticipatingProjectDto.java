package com.tk.app.domain.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParticipatingProjectDto {
	private Long projectId; // 참여프로젝트ID
	private Long workspaceId; // 워크번호
	private String subject; // 프로젝트명
	
	// 프로젝트 상태
	private Integer projectStatus;
	
	// 프로젝트 기간
	private String startAt;
	private String endAt;
	
	// BigDecimal: 숫자를 정밀하게 저장하고 표현하는 방법
	private BigDecimal progress; // 진행률
}
