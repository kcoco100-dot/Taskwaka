package com.tk.app.chat;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.tk.app.domain.dto.SessionInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
	private final UserSessionRegistry sessionRegistry;
	
	// Spring WebSocket(STOMP) 환경에서 서버가 클라이언트에게 메시지를 보내기 위한 객체
	private final SimpMessagingTemplate messagingTemplate;
	
	@EventListener // 애플리케이션 이벤트를 특정 메소드가 자동으로 처리하도록 하는 애노테이션
	public void handleConnect(SessionConnectEvent event) {
		// 클라이언트가 서버에 연결을 시도할경우 발생하는 SessionConnectEvent 처리
		// SessionConnectEvent : 클라이언트가 connect 메시지를 보낼때 발생하는 이벤트
		
		// StompHeaderAccessor : STOMP 메시지의 헤더 정보를 쉽게 읽을 수 있게 하는 클래스
		// 이벤트로 전달된 헤더를 StompHeaderAccessor 상태로 꺼냄
		StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
		
		// 세션에서 로그인 정보 추출
		Map<String, Object> sessionAttributes = sha.getSessionAttributes();
		
		if(sessionAttributes != null && sessionAttributes.containsKey("member")) {
			SessionInfo info = (SessionInfo)sessionAttributes.get("member");
			
			// 로그인 유저 등록
			sessionRegistry.add(info.getMember_id(), info.getName());
			
			// 접속 유저들에게 온라인 상태임을 알림
			Map<String, Object> payload = new HashMap<>();
			payload.put("uid", info.getMember_id());
			payload.put("nickname", info.getName());
			
			// 클라이언트에게 전송
			messagingTemplate.convertAndSend("/topic/online", (Object)payload);
		}
		
		
	}
	
	@EventListener
	public void handleDisconnect(SessionDisconnectEvent event) {
		// 클라이언트가 서버에 연결을 해제할경우 발생하는 SessionDisconnectEvent 처리
		
		StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionAttributes = sha.getSessionAttributes();
		if(sessionAttributes != null && sessionAttributes.containsKey("member")) {
			SessionInfo info = (SessionInfo)sessionAttributes.get("member");
			
			// 온라인 유저 삭제
			sessionRegistry.remove(info.getMember_id());
			
			// 온라인인 모든 유저에게 오프라인 상태를 알림
			Map<String, Object> payload = Map.of(
					"uid", info.getMember_id(),
					"nickname", info.getName()
					);
			
			messagingTemplate.convertAndSend("/topic/offline", (Object)payload);
					
		}
		
		
	}
}
