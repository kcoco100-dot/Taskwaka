package com.tk.app.admin.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminWorkspaceMemberDto {
	private String login_id;
	private String subject;
	private String role_name;
}
