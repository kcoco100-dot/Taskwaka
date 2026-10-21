package com.tk.app.domain.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageCalendarDto {
	private Long projectId;
	private Long taskId;
	private String projectName;
	private String title;
	private String content;
	private String startAt;
	private String endAt;
	private Integer status;
	private BigDecimal progress;
	private String assignee;
}
