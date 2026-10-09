package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageDto {
	private Long project_id;
	private Long workspace_id;
	private Long member_id;
	
	private String subject;
	private String content;
	private String start;
	private String end;
	private String archive;
	private String created;
	private String updated;
	private String deleted;
	
	private int status;
	private int progress;
	
	
	private Long project_member_id;
	private Long workspace_member_id;
	
	private int role;
	
	private String joined;
	private String left;
	private String update;
	
}
