package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class MomDto {
	private Long meetingMinuteId;
	private Long projectId;
	private Long subTaskId;
	private Long memberId;
	private String title;
	private String content;
	private String meeting;
	private String created;
	private String updated;
	private String deleted;
}
