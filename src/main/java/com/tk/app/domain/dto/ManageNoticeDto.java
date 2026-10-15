package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageNoticeDto {
	private long notice_id;
	private long workspace_id;
	private long task_id;
	private long member_id;
	
	private String notice_type;
	private String title;
	private String content;
	private String created;
	private String updated;
	private String deleted;
	
	private int status;
	private int view_count;
	
	
	private long notice_file_id;
	private int file_size;
	
	private String original_name; 
	private String stored_name;
	private String file_path;
	private String uploaded;
	
	private long project_id;
	private int file_count;
	private String member_name;
	
}
