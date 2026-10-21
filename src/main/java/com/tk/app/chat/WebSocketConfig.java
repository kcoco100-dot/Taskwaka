package com.tk.app.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	// 엔드포인트(설정주소) 등록
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws-workchat")
			.setAllowedOriginPatterns("*")
			.addInterceptors(new HttpSessionHandshakeInterceptor())
			.withSockJS();
	}
	
	// 메시지 전송 및 구독 주소 설정
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		
		//서버가 클라이언트로
		registry.enableSimpleBroker("/topic", "/queue");
		
		// 클라이언트가 서버로
		registry.setApplicationDestinationPrefixes("/app");
		
		// 특정 사용자에게만 메시지
		registry.setUserDestinationPrefix("/user");
		
	}
}
