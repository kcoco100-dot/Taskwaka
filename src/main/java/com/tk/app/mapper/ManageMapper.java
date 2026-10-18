package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageDto;

@Mapper
public interface ManageMapper {
	public Long findByDomainKey(String domainKey) throws Exception;
	
	public List<ManageDto> findById(Map<String, Object> map);
    public List<ManageDto> findPrevPost(Map<String, Object> map);
    public List<ManageDto> findNextPost(Map<String, Object> map);
    
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException;
	public List<ManageDto> listProjectManage(Map<String, Object> map) throws SQLException;
	public int dataCount(Map<String, Object> map);
	
	int getProjectCount(Long workspaceId) throws Exception;
	public List<ManageDto> getProjectDetail(Map<String, Object> map) throws Exception;
	
}
