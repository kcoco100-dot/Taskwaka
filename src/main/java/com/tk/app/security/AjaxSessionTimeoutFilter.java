package com.tk.app.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
  - isAjaxRequest() 메소드를 이용,  변수 ajaxHeader가 가지고 있는 문자열과
    같은 이름이 요청헤더에 포함되어 있고 "AJAX"라는 요청헤더가 가지고 있는 값이 true 이면
    try~catch 문이 있는 영역을 실행한다.
  - AJAX에서 스프링 시큐리티에 의해 예외가 발생하면 catch문에서 응답에러를 사용자 측에 보낸다.
*/
public class AjaxSessionTimeoutFilter implements Filter {
	private String ajaxHeader;
	
	@Override
	public void init(FilterConfig config) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (isAjaxRequest(req)) {
            try {
                chain.doFilter(req, resp);
            } catch (AccessDeniedException e) {
            	// 권한이 없거나 로그인이 되지 않는경우 모두 AccessDeniedException이 발생
                resp.sendError(HttpServletResponse.SC_FORBIDDEN); // 403
            } catch (AuthenticationException e) {
            	resp.sendError(HttpServletResponse.SC_UNAUTHORIZED); // 401
            } 
        } else {
            chain.doFilter(req, resp);
        }
	}
	
	@Override
	public void destroy() {
	}
	
	public void setAjaxHeader(String ajaxHeader) {
        this.ajaxHeader = ajaxHeader;
    }
	
	private boolean isAjaxRequest(HttpServletRequest req) {
        return req.getHeader(ajaxHeader) != null
                && req.getHeader(ajaxHeader).equals(Boolean.TRUE.toString());
    }
}
