package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.AdminMemberDto;
import com.tk.app.admin.domain.dto.AdminWorkspaceMemberDto;

@Mapper
public interface AdminMemberMapper {
	public List<AdminMemberDto> findAllMembers();
	public List<AdminWorkspaceMemberDto>  findMemberWorkspaces(long member_id);
	public AdminMemberDto findMemberById(long member_id);
	
	public List<AdminMemberDto> searchMembers(String keyword);
	public List<AdminMemberDto> searchMembersByStatus(@Param("keyword") String keyword, @Param("status") Integer status);
	public int updateMemberStatus(@Param("member_id") long member_id, @Param("status") int status);
}
