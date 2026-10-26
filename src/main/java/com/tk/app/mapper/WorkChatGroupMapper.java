package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.WorkChatGroupDto;
import com.tk.app.domain.dto.WorkChatMemberDto;

@Mapper
public interface WorkChatGroupMapper {
	public Long findWorkIdByDomain(String domainKey);

	public int insertGroupRoom(WorkChatGroupDto dto);
	public int updateGroupRoomName(WorkChatGroupDto dto);
	
	public List<WorkChatMemberDto> findMyGroupRooms(WorkChatMemberDto dto);
	

}
