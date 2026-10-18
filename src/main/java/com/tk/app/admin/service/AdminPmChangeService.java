package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.AdminPmChangeDto;

public interface AdminPmChangeService {
	public List<AdminPmChangeDto> findAllPmChanges();
	public int processPmChange(AdminPmChangeDto dto);
}
