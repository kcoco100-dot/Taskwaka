package com.tk.app.chat;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
	private final UserSessionRegistry sessionRegistry;
	
	// Spring WebSocket(STOMP) 환경에서 서버가 클라이언트에게 메시지를 보내기 위한 객체
	private final SimpMessagingTemplate messagingTemplate;
	private final SimpUserRegistry userRegistry;
	
	@EventListener 
	public void handleConnect(SessionConnectEvent event) {

		StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
		

		Map<String, Object> sessionAttributes = sha.getSessionAttributes();
		
		if(sessionAttributes != null && sessionAttributes.containsKey("member")) {
			SessionInfo info = (SessionInfo)sessionAttributes.get("member");
			
			
			sessionRegistry.add(info.getMember_id(), info.getName());
			
			
			Map<String, Object> payload = new HashMap<>();
			payload.put("uid", info.getMember_id());
			payload.put("nickname", info.getName());
			
			
			messagingTemplate.convertAndSend("/topic/online", (Object)payload);
		}
		
		
	}
	
	@EventListener
	public void handleDisconnect(SessionDisconnectEvent event) {
		
		
		StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionAttributes = sha.getSessionAttributes();
		if(sessionAttributes != null && sessionAttributes.containsKey("member")) {
			SessionInfo info = (SessionInfo)sessionAttributes.get("member");
			
			
			sessionRegistry.remove(info.getMember_id());
			
			
			Map<String, Object> payload = Map.of(
					"uid", info.getMember_id(),
					"nickname", info.getName()
					);
			
			messagingTemplate.convertAndSend("/topic/offline", (Object)payload);
					
		}
	}
		
		 @TransactionalEventListener
		 public void onMessageSaved(WorkChatMessageSavedEvent event) {

	        try {
	            List<Long> memberIds = event.getParticipantMemberIds();

	            for (SimpUser user : userRegistry.getUsers()) {

	                Principal principal = user.getPrincipal();

	                if (! (principal instanceof Authentication auth)) {
	                    continue;
	                }

	                if (! (auth.getPrincipal()
	                        instanceof CustomUserDetails details)) {
	                    continue;
	                }

	                Long connectedMemberId = details.getMember().getMember_id();

	                if (connectedMemberId.equals(event.getSenderMemberId())) {
	                    continue;
	                }

	                if (! memberIds.contains(connectedMemberId)) {
	                    continue;
	                }

	                messagingTemplate.convertAndSendToUser(
	                    user.getName(),
	                    "/queue/workchat",
	                    event.getChatRoomId()
	                );

	                log.info("워크챗 알림 전송 - 방: {}, 회원: {}",
	                    event.getChatRoomId(),
	                    connectedMemberId);
	            	}

	        	} catch (Exception e) {
	        		log.error("워크챗 실시간 알림 처리 실패", e);
	        	}
		
	}
}
