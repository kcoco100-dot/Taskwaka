package com.tk.app.security;

import java.io.IOException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import com.tk.app.service.MemberService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginFailureHandler implements AuthenticationFailureHandler {
	private final MemberService memberService;

	private String defaultFailureUrl = "/member/login?error";

	public LoginFailureHandler(MemberService memberService) {
		this.memberService = memberService;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {

		String login_id = request.getParameter("login_id");

		String errorMsg = "아이디 또는 패스워드가 일치하지 않습니다.";

		try {
			if (exception instanceof BadCredentialsException) {
				// 패스워드가 일치하지 않는 경우

				int cnt = memberService.checkFailureCount(login_id);
				if (cnt < 5) {
					memberService.updateFailureCount(login_id);
				}

				

				errorMsg = "아이디 또는 패스워드가 일치하지 않습니다.";
			} else if (exception instanceof InternalAuthenticationServiceException) {
				// 아이디가 일치하지 않는 경우

				errorMsg = "아이디 또는 패스워드가 일치하지 않습니다.";
			} else if (exception instanceof DisabledException) {
				// 인증 거부 - 계정 비활성화

				errorMsg = "계정이 비활성화 되었습니다. 관리자에게 문의 하세요.";
			} else if (exception instanceof LockedException) {
				errorMsg = "로그인 5회 실패로 계정이 잠겼습니다.";
			}
		} catch (Exception e) {
			log.info("onAuthenticationFailure : " + errorMsg, e);
		}


		String redirectUrl = "/member/login?error";
		
		try {
			if (exception instanceof DisabledException) {
		        redirectUrl = "/member/login?error=disabled";
			
			} else if(exception instanceof LockedException) {
		        redirectUrl = "/member/login?error=locked";
		        
		    } else if (exception instanceof BadCredentialsException
		            && login_id != null
		            && !login_id.isBlank()
		            && memberService.checkFailureCount(login_id) >= 5) {
		
		        redirectUrl = "/member/login?error=locked";
		    }
		
		} catch (Exception e) {
		    log.error("로그인 잠금 상태 조회 실패", e);
		}


		response.sendRedirect(request.getContextPath() + redirectUrl);

	}

	public void setDefaultFailureUrl(String defaultFailureUrl) {
		this.defaultFailureUrl = defaultFailureUrl;
	}

}
