package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.ParticipatingProjectDto;

@Mapper
public interface ParticipatingProjectMapper {

	// 참여 프로젝트 조회: 여러개의 프로젝트를 참여중일 수 있으므로 List
	List<ParticipatingProjectDto> findParticipatingProjects(
			@Param("workspaceId") long workspaceId,
			@Param("memberId") long memberId);
}
