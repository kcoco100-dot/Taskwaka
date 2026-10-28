package com.tk.app.service;

import java.util.List;

import com.tk.app.domain.dto.WorkChatGroupDto;
import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;

public interface WorkChatRoomService {
	public Long startDirectRoom(String domainKey, Long workId, Long loginMemberId, Long targetMemberId) throws Exception;
	
	public List<WorkChatMemberDto> findMyDirectRooms(String domainKey, Long loginMemberId)throws Exception; 
	public List<WorkChatMessageDto> findRoomMessages(String domainKey, Long chatRoomId, Long loginMemberId ) throws Exception;
	
	public void sendMessage(WorkChatMessageDto dto) throws Exception;
	public void markRoomAsRead(WorkChatMessageDto dto) throws Exception;
	
	public Long createGroupRoom(WorkChatGroupDto dto) throws Exception;

	public List<WorkChatMemberDto> findMyGroupRooms(WorkChatMemberDto dto) throws Exception;
	
	public String renameGroupRoom(WorkChatGroupDto dto) throws Exception;
	
	public List<WorkChatMemberDto> findGroupRoomMembers(WorkChatMemberDto dto) throws Exception;
	
	public void inviteGroupMembers(WorkChatGroupDto dto) throws Exception;
	
	public void leaveGroupRoom(WorkChatMemberDto dto) throws Exception;
	
	public void transferGroupMaster(WorkChatMemberDto dto) throws Exception;
	
	public void leaveDirectRoom(WorkChatMemberDto dto) throws Exception;
	
}
