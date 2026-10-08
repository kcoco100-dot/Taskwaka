package com.tk.app.domain.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MemberDto {
	private Long member_id;
	private String login_id;
	private String password;
	private String sns_provider;
	private String sns_id;
	private int userLevel;
	private int enabled;
	private String created_at;
	private String update_at;
	private String last_login;
	private int failure_cnt;

	private String name;
	private String birth;
	private String email;
	private int receive_email;
	private String tel;
	private String profile_photo;
	private String zip;
	private String addr1;
	private String addr2;
	private String ipAddr;
	
	private MultipartFile selectFile;
	
	private String authority;
	private String oldAuthority;
	
	private String rt_value;
	
	private long num;
	private int status_code;
	private String memo;
	private long register_id;
	
	private int status;
	private int fail_cnt;
	private String blocked_until;
	
	private String phone;
	private String role;
	
	private Integer workspaceRole;
}
