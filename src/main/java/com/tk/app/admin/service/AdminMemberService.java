package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.AdminMemberDto;

public interface AdminMemberService {
	public List<AdminMemberDto> findAllMembers();
}
