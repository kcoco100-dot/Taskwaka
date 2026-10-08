package com.tk.app.security;

import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.MemberDto;
import com.tk.app.domain.dto.SessionInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class LoginSnsSuccessHandler {
	public void forceLogin(MemberDto dto) throws Exception {
		try {
			SessionInfo info = SessionInfo.builder()
					.member_id(dto.getMember_id())	
					.login_id(dto.getSns_id())
					.name(dto.getName())
					.email(dto.getEmail())
					.userLevel(NumericRoleGranted.getUserLevel("USER"))
					.login_type(dto.getSns_provider())
					.build();
			
			CustomUserDetails userDetails = CustomUserDetails.builder()
					.sessionInfo(info)
					.disabled(false)
					.roles(Arrays.asList("USER"))
					.build();
			
			// 시큐리티 로그인 처리
			Authentication authentication = 
					new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities());
			
			SecurityContext context = SecurityContextHolder.getContext();
			context.setAuthentication(authentication);
			
			// HttpSession 에 SecurityContext 저장
			// SPRING_SECURITY_CONTEXT : 시큐리티가 세션에 인증 정보를 찾을 때 사용하는 표준키
			HttpServletRequest request = RequestUtils.getCurrentRequest();
			HttpSession session = request.getSession(true);
			session.setAttribute("SPRING_SECURITY_CONTEXT", context);
			
		} catch (Exception e) {
			log.info("forceLogin : ", e);
			
			throw e;
		}
	}
}
