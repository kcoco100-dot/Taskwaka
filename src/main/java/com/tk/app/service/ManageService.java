package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageDto;

public interface ManageService {
	public Long findByDomainKey(String domainKey) throws Exception;
	public List<ManageDto> listProjectManage(Map<String, Object> map) throws SQLException;
	public Long getOwnWorkspaceMemberId(Map<String, Object> map);
	public Map<String, Object> getManageCalendarData(Map<String, Object> map) throws Exception;
	public void assignOrUpdateProjectMember(Map<String, Object> map) throws Exception;
	public void excludeProjectMember(Map<String, Object> map) throws Exception;
	public void excludeWorkspaceMember(Map<String, Object> map) throws Exception;
	
	public Map<String, Object> getProjectMemberForm(String domainKey, Long actorId, Long projectId) throws Exception;
	public int saveProjectMembers(String domainKey, Long actorId, Map<String, Object> map) throws Exception;
}
