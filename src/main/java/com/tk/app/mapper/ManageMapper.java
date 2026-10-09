package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageDto;

@Mapper
public interface ManageMapper {
	public void insertProject(ManageDto dto) throws SQLException;
	public void updateProject(ManageDto dto) throws SQLException;
	public void deleteProject(ManageDto dto) throws SQLException;
	
	public List<ManageDto> findById(Long project_id);
	
}
