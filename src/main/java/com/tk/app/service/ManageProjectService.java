package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageTaskDto;

public interface ManageProjectService {
	public boolean canCreateProject(Long workspaceId, Long memberId) throws SQLException;
	public boolean canManageProject(Map<String, Object> map) throws SQLException;

	public Long findByDomainKey(String domainKey) throws Exception;

	public void insertProject(ManageDto dto) throws SQLException;
	public void updateProject(Map<String, Object> map) throws SQLException;
	public void deleteProject(Map<String, Object> map) throws SQLException;

	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException;
	public int dataCount(Map<String, Object> map) throws SQLException;
	public ManageDto getProjectDetail(Map<String, Object> map) throws SQLException;

	public List<ManageTaskDto> listManageTasks(Map<String, Object> map) throws SQLException;
	public List<Map<String, Object>> listManageTaskTL(Map<String, Object> map) throws SQLException;
	
	public void insertManageTask(ManageTaskDto dto, Map<String, Object> context) throws SQLException;
	public void updateManageTask(ManageTaskDto dto, Map<String, Object> context) throws SQLException;
	public void deleteManageTask(Map<String, Object> map) throws SQLException;
	
	public ManageDto selectPrevProject(Map<String, Object> map) throws SQLException;
    public ManageDto selectNextProject(Map<String, Object> map) throws SQLException;
}
