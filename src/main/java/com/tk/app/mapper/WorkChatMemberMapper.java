package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.WorkChatMemberDto;

@Mapper
public interface WorkChatMemberMapper {

	public List<WorkChatMemberDto> findMemberByWorkId(Long workId);
	public List<WorkChatMemberDto> findMemberProjectsByWorkId(Long workId);
	
	
}
