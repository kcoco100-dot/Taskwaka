package com.tk.app.admin.domain.dto;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminMemberDto {
	private long member_id;
	private String login_id;
	private int status;
	private Date joined;
	private String name;
	private String email;
}
