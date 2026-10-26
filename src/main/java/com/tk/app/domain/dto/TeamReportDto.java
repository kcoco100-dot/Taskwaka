package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class TeamReportDto {
	private Long teamReportId;
	private Long taskId;
	private Long projectMemberId;
	private Long projectId;
	private String reportType;
	private String title;
	private String content;
	private Double progress;
	private Long status;
	private String created;
	private String updated;
	private String deleted;
}
