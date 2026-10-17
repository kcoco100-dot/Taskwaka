package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GrowthLevelDto {
	private long growthLevelId; // 성장단계번호
	private int levelNo; // 화면에 표시할 단계 번호
	private String levelName; // 단계 이름
	private long minExp; // 최소 경험치
	private Long maxExp; // 최대 경험치
	private String treeImagePath; // 나무 이미지 경로
}
