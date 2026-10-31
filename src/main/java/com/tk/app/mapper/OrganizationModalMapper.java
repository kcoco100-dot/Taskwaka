package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.OrganizationModalDto;
import com.tk.app.domain.dto.OrganizationTaskDto;

@Mapper
public interface OrganizationModalMapper {

	public Long findActiveWorkId(OrganizationModalDto dto);
	public List<OrganizationModalDto> findMyProjects(OrganizationModalDto dto);
	public int countAccessibleProject(OrganizationModalDto dto);
	public List<OrganizationModalDto> findProjectMembers(OrganizationModalDto dto);
	public List<OrganizationTaskDto> findProjectTasks(OrganizationModalDto dto);
	public List<OrganizationTaskDto> findProjectSubTasks(OrganizationModalDto dto);
	public List<OrganizationTaskDto> findTaskMembers(OrganizationModalDto dto);
	public List<OrganizationTaskDto> findSubTaskMembers(OrganizationModalDto dto);
}
