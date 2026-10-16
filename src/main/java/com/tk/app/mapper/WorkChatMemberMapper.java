package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.WorkChatMemberDto;

@Mapper
public interface WorkChatMemberMapper {

	List<WorkChatMemberDto> findMemberByWorkId(Long workId);
}
