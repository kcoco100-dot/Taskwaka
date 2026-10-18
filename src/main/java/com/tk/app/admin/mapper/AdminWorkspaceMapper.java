package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.admin.domain.dto.AdminWorkspaceDto;

@Mapper
public interface AdminWorkspaceMapper {

	public List<AdminWorkspaceDto> findAllWorkspaces();
	public int updateWorkspaceStatus(AdminWorkspaceDto dto);
}
