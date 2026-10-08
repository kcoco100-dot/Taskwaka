package com.tk.app.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginFailureHandler implements AuthenticationFailureHandler {
	private final MemberService memberService;
	
	private String defaultFailureUrl;
	
	public LoginFailureHandler(MemberService memberService) {
		this.memberService = memberService;
	}
	
	
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		
		String login_id = request.getParameter("login_id");
		
		String errorMsg = "아이디 또는 패스워드가 일치하지 않습니다.";
		
		try {
			if(exception instanceof BadCredentialsException) {
				// 패스워드가 일치하지 않는 경우
				
				int cnt = memberService.checkFailureCount(login_id);
				if(cnt <= 4) {
					memberService.updateFailureCount(login_id);
				}
				
				if(cnt >= 4) {
					MemberDto dto = memberService.findById(login_id);
					
					// 계정 비활성화
					Map<String, Object> map = new HashMap<>();
					map.put("enabled", 0);
					map.put("member_id", dto.getMember_id());
					
					memberService.updateMemberEnabled(map);
					
					// 계정 비활성 상태 저장
					MemberDto vo = new MemberDto();
					vo.setMember_id(dto.getMember_id());
					vo.setRegister_id(dto.getMember_id());
					vo.setStatus_code(1);
					vo.setMemo("패스워드 5회 이상 실패");
					memberService.insertMemberStatus(vo);
				}
				
				errorMsg = "아이디 또는 패스워드가 일치하지 않습니다.";
			} else if(exception instanceof InternalAuthenticationServiceException) {
				// 아이디가 일치하지 않는 경우
				
				errorMsg = "아이디 또는 패스워드가 일치하지 않습니다.";
			} else if(exception instanceof DisabledException) {
				// 인증 거부 - 계정 비활성화
				
				errorMsg = "계정이 비활성화 되었습니다. 관리자에게 문의 하세요.";
			}
		} catch (Exception e) {
			log.info("onAuthenticationFailure : " + errorMsg, e);
		}
		
		
		response.sendRedirect(defaultFailureUrl);
	}


	public void setDefaultFailureUrl(String defaultFailureUrl) {
		this.defaultFailureUrl = defaultFailureUrl;
	}
	
}
