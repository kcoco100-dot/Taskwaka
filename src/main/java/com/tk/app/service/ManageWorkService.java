package com.tk.app.service;

import java.util.List;

import com.tk.app.domain.dto.ManageWorkDto;

public interface ManageWorkService {
	public int findWorkRole(Long workspaceId, Long memberId) throws Exception;
	public int dataCount(Long workspaceId, Long actorId, String kwd) throws Exception;
	public List<ManageWorkDto> listWorkMember(Long workspaceId, Long actorId, String kwd, int current_page, int size) throws Exception;
	public void updateWorkRole(Long workspaceId, Long actorId, Long workspaceMemberId, int role) throws Exception;
	
	public int requestChange(Long workspaceId, Long memberId, Long newMemberId, String reason) throws Exception;
	
}
