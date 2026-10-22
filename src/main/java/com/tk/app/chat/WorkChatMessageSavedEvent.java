package com.tk.app.chat;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WorkChatMessageSavedEvent {
	private final Long chatRoomId;

    private final Long senderMemberId;

    private final List<Long> participantMemberIds;
}
