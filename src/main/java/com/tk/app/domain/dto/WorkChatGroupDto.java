package com.tk.app.domain.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkChatGroupDto {
	private Long chatRoomId;
	
    private String domainKey;
    private Long loginMemberId;

    private Long workId;
    private Long myWorkId;

    private String roomName;
    private List<Long> memberIds;
}
