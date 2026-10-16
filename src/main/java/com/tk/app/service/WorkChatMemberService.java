package com.tk.app.service;

import java.util.List;

import com.tk.app.domain.dto.WorkChatMemberDto;

public interface WorkChatMemberService {
	
    List<WorkChatMemberDto> getMembers(Long workId, Long loginMemberId);
}
