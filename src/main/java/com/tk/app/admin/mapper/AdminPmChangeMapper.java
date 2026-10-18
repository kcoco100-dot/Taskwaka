package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.admin.domain.dto.AdminPmChangeDto;

@Mapper
public interface AdminPmChangeMapper {
	 public List<AdminPmChangeDto> findAllPmChanges();
	 
	 public int updatePmChangeStatus(AdminPmChangeDto dto);
	 public int updateWorkspacePm(AdminPmChangeDto dto);
	 public int updateOldPmRole(AdminPmChangeDto dto);
	 public int updateNewPmRole(AdminPmChangeDto dto);
	 
	 public AdminPmChangeDto findPmChangeById(long pm_change_id);
	 public int countNewPmMember(AdminPmChangeDto dto);
	 public List<Long> findDtProjectIds(AdminPmChangeDto dto);
	 public int updateOldPmProjectRole(AdminPmChangeDto dto);
	 public int updateNewPmProjectRole(AdminPmChangeDto dto);
	 public int insertNewPmProjects(AdminPmChangeDto dto);
	 public int deactivateOldPmProjects(AdminPmChangeDto dto);
	 public int countNewPmDt(AdminPmChangeDto dto);
	 public int countAllProjects(AdminPmChangeDto dto);
	 public int countNewPmProjects(AdminPmChangeDto dto);
	 public int reactivateNewPmProjects(AdminPmChangeDto dto);
}
