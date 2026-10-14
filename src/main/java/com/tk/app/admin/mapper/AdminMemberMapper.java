package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.admin.domain.dto.AdminMemberDto;

@Mapper
public interface AdminMemberMapper {
	public List<AdminMemberDto> findAllMembers();
}
