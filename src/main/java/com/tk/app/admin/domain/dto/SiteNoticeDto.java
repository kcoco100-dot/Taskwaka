package com.tk.app.admin.domain.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteNoticeDto {
	private long notice_id;
	private long member_id;
	private String title;
	private String content;
	private int status;
	private Date created;
	private Date updated;
	private String member_name;
	private int file_count;
	private String name;
}
