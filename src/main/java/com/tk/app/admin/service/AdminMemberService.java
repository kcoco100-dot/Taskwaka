package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.AdminMemberDto;
import com.tk.app.admin.domain.dto.AdminWorkspaceMemberDto;

public interface AdminMemberService {
	public List<AdminMemberDto> findAllMembers();
	public List<AdminWorkspaceMemberDto>  findMemberWorkspaces(long member_id);
	public AdminMemberDto findMemberById(long member_id);
	public List<AdminMemberDto> searchMembers(String keyword);
	public List<AdminMemberDto> searchMembersByStatus(String keyword, Integer status);
	public int updateMemberStatus(long member_id, int status); 
}
