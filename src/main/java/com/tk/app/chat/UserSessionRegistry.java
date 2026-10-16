package com.tk.app.chat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// 접속 유저의 정보를 저장하는 클래스

@Component
public class UserSessionRegistry {
	private static final Map<Long, String> onlineUsers = new ConcurrentHashMap<>();
	
	public void add(Long member_id, String name) {
		onlineUsers.put(member_id, name);
	}
	
	public void remove(Long member_id) {
		onlineUsers.remove(member_id);
	}
	
	public Map<Long, String> getAllOnlineUsers(){
		return onlineUsers;
	}
}
