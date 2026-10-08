package com.tk.app.security;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginSuccessHandler implements AuthenticationSuccessHandler {
	private final MemberService memberService;
	
	private RequestCache requestCache = new HttpSessionRequestCache();
			// 로그인 되지 않은 상태에서 보호된 url을 접근할 경우, 
			// 원래 요청했던 url을 저장해 두고, 로그인 후 다시 그 url로 보내주는 기능
	private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
			// 로그인 후 어디로 리다이렉트 할지 결정
	private String defaultUrl;
	
	
	public LoginSuccessHandler(MemberService memberService) {
		this.memberService = memberService;
	}
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {


		try {
			// 로그인 날짜 변경
			memberService.updateLastLogin(authentication.getName());
			
			// 패스워드 변경이 90일이 지난 경우 패스워드 변경 폼으로 이동
			MemberDto dto = memberService.findById(authentication.getName());
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime target = LocalDateTime.parse(dto.getUpdate_at(), dtf);
			long daysBetween = ChronoUnit.DAYS.between(target, now);
			
			if(daysBetween >= 90) {
				String targetUrl = "/member/updatePwd";
				redirectStrategy.sendRedirect(request, response, targetUrl);
				
				return;
			}
			
		} catch (Exception e) {
		}
		
		
		resultRedirectStrategy(request, response, authentication);
	}
	
	protected void resultRedirectStrategy(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		
		SavedRequest savedRequest = requestCache.getRequest(request, response);
		if(savedRequest != null) {
			String targetUrl = savedRequest.getRedirectUrl();
			redirectStrategy.sendRedirect(request, response, targetUrl);
		} else {
			redirectStrategy.sendRedirect(request, response, defaultUrl);
		}
		
		
	}
	
	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}

}
