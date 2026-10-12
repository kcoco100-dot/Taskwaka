package com.tk.app.service;

import com.tk.app.domain.dto.WorkCreateDto;

public interface WorkCreateService {
	public void insertWork(WorkCreateDto dto, Long LoginMemberId) throws Exception;
}
