package com.tk.app.mapper;

import java.util.List;

import com.tk.app.domain.dto.WorkChatGroupDto;
import com.tk.app.domain.dto.WorkChatMemberDto;

public interface WorkChatGroupMapper {
	public Long findWorkIdByDomain(String domainKey);

	int insertGroupRoom(WorkChatGroupDto dto);

	List<WorkChatMemberDto> findMyGroupRooms(WorkChatMemberDto dto);
}
