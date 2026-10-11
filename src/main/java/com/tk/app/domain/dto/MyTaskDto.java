package com.tk.app.domain.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MyTaskDto {
	private Long taskId; // 업무번호
	private Long projectId; // 프로젝트번호
	private String subject; // 프로젝트명
	private String title; // 업무명
	
	private String content; // 업무내용
	private int taskLevel; // 업무단계
	
	private String startAt; // 시작일
	private String endAt; // 종료일
	
	private Integer status; // 진행상태 0=할일, 1=진행중, 2=검토중, 3=완료
	private BigDecimal progress; // 진행률 0~100
	
}
