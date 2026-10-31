package com.tk.app.chat;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {
	private final UserSessionRegistry sessionRegistry;
	private final SimpMessagingTemplate messagingTemplate;
	
	
	@GetMapping("/chat/main")
	public String handleChatMessage() {
		return "chat/main";
	}
	
	@GetMapping("/chat/onlineUsers")
	public ResponseEntity<?> getUserList(){
		// 온라인 유저
		return ResponseEntity.ok(sessionRegistry.getAllOnlineUsers());
	}
	
	@MessageMapping("/chat.send")
	@SendTo("/topic/public")
	public ChatMessage sendMessage(ChatMessage message) {
		return message;
	}
	
	@MessageMapping("/chat.whisper")
	public void privateMessage(ChatMessage message, Principal principal) {
        if (principal == null || message.getReceiver() == null || message.getReceiver().isBlank()) return;
        message.setSender(principal.getName());
        messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/private", message);
	}
	
	@MessageMapping("/chat.test")
	public void sendMsg(ChatMessage msg, Principal principal) {
		if(principal == null) {
			return;
		}
		
		String loginId = principal.getName();
		System.out.println(loginId);
		
		if(msg == null || msg.getMessage() == null || msg.getMessage().isBlank()) {
			return;
		}
		
		ChatMessage result = new ChatMessage();
		
		result.setMessage(msg.getMessage());
		result.setSender(principal.getName());
		
		messagingTemplate.convertAndSend("/topic/chat.test", result);
		
		
		
	}
	
}
