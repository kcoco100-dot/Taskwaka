package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.NoticeDto;

@Mapper
public interface NoticeMapper {

	// 공개되있는 공지 목록 조회
	List<NoticeDto> findWorkspaceNotice(
			@Param("workspaceId") long workspaceId);
}
