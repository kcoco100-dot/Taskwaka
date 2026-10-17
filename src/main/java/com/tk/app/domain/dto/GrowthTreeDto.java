package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GrowthTreeDto {
	private long workspaceId; // 어느 워크 성장나무인지 번호로 구분
	private long growthLevelId; // 현재 성장단계 실제 번호
	private int levelNo; // 단계번호
	private String levelName; // 단계이름
	private long totalExp; // 누적 성장 경험치
	private String treeImagePath; // 나무 이미지 경로
}
