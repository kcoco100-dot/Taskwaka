package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;

@Mapper
public interface WorkChatRoomMapper {

	public Long findWorkMemberId(WorkChatMemberDto dto) throws SQLException;
	public Long findDirectRoom(WorkChatMemberDto dto) throws SQLException;
	
	public Long nextRoomId() throws SQLException;
	public void insertDirectRoom(WorkChatMemberDto dto) throws SQLException;
	public void insertRoomMember(WorkChatMemberDto dto) throws SQLException;
	
	public int matchDomain(WorkChatMemberDto dto);
	public List<WorkChatMemberDto> findMyDirectRooms(WorkChatMemberDto dto);
	
	public int countReadableRoom(WorkChatMessageDto dto);
	public List<WorkChatMessageDto> findRoomMessages(WorkChatMessageDto dto);
	
	public Long findSenderRoomMemberId(WorkChatMessageDto dto);
	public int insertRoomMessage(WorkChatMessageDto dto); 	
	public List<Long> findActiveRoomMemberIds(WorkChatMessageDto dto);
	
	public WorkChatMessageDto findLatestRoomMessage(WorkChatMemberDto dto);
	public int countUnreadMessages(WorkChatMemberDto dto);
	public int updateLastReadMessage(WorkChatMessageDto dto);
	
	
}
