package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.repository.ProjectTeamView;

public interface ManageService {
	public Long findByDomainKey(String domainKey) throws Exception;
	
	public List<ManageDto> findById(Map<String, Object> map);
    public List<ManageDto> findPrevPost(Map<String, Object> map);
    public List<ManageDto> findNextPost(Map<String, Object> map);
	
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException;
	public List<ManageDto> listProjectManage(Map<String, Object> map) throws SQLException;
	public int dataCount(Map<String, Object> map) throws Exception;
	
	public List<ProjectTeamView> listWorkspaceMembers(Long workspaceId) throws Exception;
	public void assignOrUpdateProjectMember(Long workspaceMemberId, Long projectId, int role) throws Exception;
	
	int getProjectCount(Long workspaceId) throws Exception;
	public List<ManageDto> getProjectDetail(Map<String, Object> map) throws Exception;
}
