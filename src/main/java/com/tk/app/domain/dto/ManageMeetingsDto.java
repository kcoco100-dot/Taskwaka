package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageMeetingsDto {
	private Long meetingMinuteId;
	private Long projectId;
	private Long subTaskId;
	private Long memberId;
	private String title;
	private String content;
	private String meetingDate;
	private String createdDate;
	private String authorName;
	private String summary;
	private boolean canDelete;
    private Long prevId;
    private String prevTitle;
    private Long nextId;
    private String nextTitle;
}
