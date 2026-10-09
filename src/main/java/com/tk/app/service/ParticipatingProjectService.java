package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.ParticipatingProjectDto;
import com.tk.app.mapper.ParticipatingProjectMapper;

@Service
public class ParticipatingProjectService {
	private final ParticipatingProjectMapper participatingProjectMapper;
	
	// mapper 객체를 전달함
	public ParticipatingProjectService(ParticipatingProjectMapper participatingProjectMapper) {
		
		// 전달받은 mapper 를 필드에 저장
		this.participatingProjectMapper = participatingProjectMapper;
	}
	
	// 참여중인 프로젝트 조회 메서드 
	public List<ParticipatingProjectDto> findParticipatingProjects(long workspaceId, long memberId) {
		
		// 보관해둔 project 호출 후 워크번호 + 회원번호 전달
		return participatingProjectMapper.findParticipatingProjects(workspaceId, memberId);
	}
}
