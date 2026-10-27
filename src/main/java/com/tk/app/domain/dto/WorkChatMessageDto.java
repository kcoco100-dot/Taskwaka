package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkChatMessageDto {

    private Long messageId;
    private Long chatRoomId;
    private Long senderMemberId;
    private String senderName;
    private String content;
    private String sent;

    
    private String domainKey;
    private Long loginMemberId;
    private Long chatRoomMemberId;
    private Integer mine;
    
    private String messageType;
}
