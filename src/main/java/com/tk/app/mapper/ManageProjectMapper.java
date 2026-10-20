package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageTaskDto;

@Mapper
public interface ManageProjectMapper {

	public Long findByDomainKey(String domainKey) throws Exception;
	public void insertProject(ManageDto dto) throws SQLException;
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException;
	public int dataCount(Map<String, Object> map) throws SQLException;
	public ManageDto getProjectDetail(Map<String, Object> map) throws SQLException;
	public int countProjectAccess(Map<String, Object> map) throws SQLException;
	public int countWorkspacePm(Map<String, Object> map) throws SQLException;

	public int updateProject(Map<String, Object> map) throws SQLException;
	public int deleteProject(Map<String, Object> map) throws SQLException;

	public List<ManageTaskDto> listManageTasks(Map<String, Object> map) throws SQLException;
	public List<Map<String, Object>> listManageTaskTL(Map<String, Object> map) throws SQLException;
	public ManageTaskDto getManageTask(Map<String, Object> map) throws SQLException;
	public int insertManageTask(ManageTaskDto dto) throws SQLException;
	public int updateManageTask(ManageTaskDto dto) throws SQLException;
	public int deleteManageTask(Map<String, Object> map) throws SQLException;
	public int countValidTaskTL(Map<String, Object> map) throws SQLException;
	public int countManageTaskReports(Map<String, Object> map) throws SQLException;
	public Long lockManageProject(Map<String, Object> map) throws SQLException;

	public int insertManageTaskMember(Map<String, Object> map) throws SQLException;
	public int deleteManageTaskMember(Map<String, Object> map) throws SQLException;
	public int updateManageTaskTL(Map<String, Object> map) throws SQLException;
}
