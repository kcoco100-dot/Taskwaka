package com.tk.app.domain.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkCreateDto {
	
	private Long workId;
	private String workSubject;
	private String workContent;
	private Long pmMemberId;
	private int status;
	private LocalDateTime createAt;
	private LocalDateTime updateAt;

	private String domainKey;
	
}
