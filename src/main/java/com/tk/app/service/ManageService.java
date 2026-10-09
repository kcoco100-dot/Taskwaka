package com.tk.app.service;

import java.sql.SQLException;

import com.tk.app.domain.dto.ManageDto;

public interface ManageService {
	public void insertProject(ManageDto dto) throws SQLException;
	public void updateProject(ManageDto dto) throws SQLException;
	public void deleteProject(ManageDto dto) throws SQLException;
}
