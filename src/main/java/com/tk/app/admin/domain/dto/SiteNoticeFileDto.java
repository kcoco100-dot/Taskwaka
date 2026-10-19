package com.tk.app.admin.domain.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteNoticeFileDto {
	private long notice_file_id;
	private long notice_id;
	private String original_name;
	private String stored_name;
	private String file_path;
	private long file_size;
	private Date uploaded;
}
