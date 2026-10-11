package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.MyTaskDto;

@Mapper
public interface MyTaskMapper {
	
	// 내 업무 리스트 조회
	// 필요한 정보 : 현재 워크 업무 조회, 로그인 회원
	List<MyTaskDto> findTasksList(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId);
}
