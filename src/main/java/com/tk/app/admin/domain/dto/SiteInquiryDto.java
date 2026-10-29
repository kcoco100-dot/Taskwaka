package com.tk.app.admin.domain.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteInquiryDto {
	private long inquiry_id;
	private long member_id;
	private String title;
	private String content;
	private int status;
	private String answer_content;
	private Date answered;
	private Date created;
	private Date updated;
	private String name;
}
