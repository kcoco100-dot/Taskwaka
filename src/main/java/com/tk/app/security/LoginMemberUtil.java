package com.tk.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.SessionInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
	## SecurityContextHolder
	- 현재 인증된 사용자(Authentication) 정보를 저장하고 꺼내는 핵심 클래스
	## Authentication
	- 현재 사용자 + 권한을 나타내는 객체
 */
public class LoginMemberUtil {
	public static SessionInfo getSessionInfo() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		
		// isAuthenticated() : 인증된 상태인지 확인
		if(authentication == null || ! authentication.isAuthenticated()) {
			return null;
		}
		
		Object principal = authentication.getPrincipal();
		
		if(principal instanceof CustomUserDetails) {
			return ((CustomUserDetails)principal).getMember();
		}
		
		return null;
	}
	
	public static void logout() {
		try {
			HttpServletRequest request = RequestUtils.getCurrentRequest();
			HttpServletResponse response = RequestUtils.getCurrentResponse();
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

			if(authentication != null) {
				// 로그아웃 시 현재 사용자의 SecurityContext와 인증 정보를 정리하는 핸들러
				new SecurityContextLogoutHandler().logout(request, response, authentication);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
