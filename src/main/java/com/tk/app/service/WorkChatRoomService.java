package com.tk.app.service;

import java.util.List;

import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;

public interface WorkChatRoomService {
	// 1:1 채팅방 조회 또는 생성
	public Long startDirectRoom(String domainKey, Long workId, Long loginMemberId, Long targetMemberId) throws Exception;
	public List<WorkChatMemberDto> findMyDirectRooms(String domainKey, Long loginMemberId)throws Exception; 
	public List<WorkChatMessageDto> findRoomMessages(String domainKey, Long chatRoomId, Long loginMemberId ) throws Exception;
	public void sendMessage(WorkChatMessageDto dto) throws Exception;
}
