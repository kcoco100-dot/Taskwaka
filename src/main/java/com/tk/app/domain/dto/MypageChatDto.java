package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MypageChatDto {

    private Long workId;
    private Long workMemberId;

    private Long chatRoomId;
    private Long chatRoomMemberId;
    private Long chatOwnerMemberId;
    private Long chatTargetMemberId;

    private String chat;
    
    private Integer chatRoomMemberStatus;
    private String systemMessage;
}
