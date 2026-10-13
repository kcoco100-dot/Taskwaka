package com.tk.app.service;

import com.tk.app.domain.dto.WorkCreateDto;

public interface WorkCreateService {
	public Long insertWork(WorkCreateDto dto, String loginId) throws Exception;
}
