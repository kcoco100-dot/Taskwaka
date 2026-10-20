package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageDto;

@Mapper
public interface ManageMapper {
	public Long findByDomainKey(String domainKey) throws Exception;
	public List<ManageDto> listProjectManage(Map<String, Object> map) throws SQLException;
	public List<Map<String, Object>> listManageCalendarTasks(Map<String, Object> map) throws SQLException;
	public Integer getManageRoleForProject(Map<String, Object> map);
	public Long getProjectIdForProjectMember(Map<String, Object> map);
	public int countOwnPmProjectMember(Map<String, Object> map);
	public Integer getTargetWorkspaceRole(Map<String, Object> map);
	public Long getOwnWorkspaceMemberId(Map<String, Object> map);
}
