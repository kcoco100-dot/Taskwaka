package com.tk.app.chat;

import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
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
			.addInterceptors(new HttpSessionHandshakeInterceptor())
			.withSockJS();
	}
	
	@Override
    public void configureClientInboundChannel(ChannelRegistration channel) {
        channel.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel input) {
                StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (headers == null) return message;
                StompCommand command = headers.getCommand();
                String dest = headers.getDestination();
                if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
                    boolean allowed = headers.getUser() != null && dest != null &&
                            (command == StompCommand.SEND ? dest.startsWith("/app/") :
                            Set.of("/user/queue/workchat", "/user/queue/private", "/topic/public",
                                    "/topic/chat.test", "/topic/online", "/topic/offline").contains(dest));
                    if (!allowed) throw new AccessDeniedException("허용되지 않는 채팅 경로입니다.");
                }
                return message;
            }
        });
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
