package com.tk.app.chat;

import lombok.Data;

@Data // 여러가지 애노테이션을 동시에 만들어준다. 사용권장하지는 않음
public class ChatMessage {
	private String sender;
	private String senderName;
	private String receiver;
	
	private String message;
}
