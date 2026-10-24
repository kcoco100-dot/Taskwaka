package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ManageTaskDto {

	private Long task_id;
	private Long project_id;
	private Long project_member_id;

	private String title;
	private String content;

	private int task_level;

	private String start_date;
	private String end_date;

	private int status;

	private double progress;
	private Double progress_weight;

	private String tl_name;
}
