package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.repository.ProjectTeamView;

public interface ManageService {
	public Long findByDomainKey(String domainKey) throws Exception;
	
	public void insertProject(ManageDto dto) throws SQLException;
	public void updateProject(ManageDto dto) throws SQLException;
	public void deleteProject(ManageDto dto) throws SQLException;
	
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException;
	public int dataCount(Map<String, Object> map) throws Exception;
	
	public List<ProjectTeamView> listWorkspaceMembers(Long workspaceId) throws Exception;
	public void assignOrUpdateProjectMember(Long workspaceMemberId, Long projectId, int role) throws Exception;
}
