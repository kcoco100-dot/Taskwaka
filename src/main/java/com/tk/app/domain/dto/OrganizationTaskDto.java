package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationTaskDto {

	private Long taskId;
	private Long subTaskId;
	private Long parentTaskId;
	private Long projectMemberId;
	private String title;
	private String assigneeName;
	private Integer status;
}
