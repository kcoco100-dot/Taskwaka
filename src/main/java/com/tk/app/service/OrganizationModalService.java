package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.OrganizationModalDto;

public interface OrganizationModalService {

	public List<OrganizationModalDto> findMyProjects(OrganizationModalDto dto);
	public Map<String, Object> findProjectTree(OrganizationModalDto dto);
}
