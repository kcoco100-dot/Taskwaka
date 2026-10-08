package com.tk.app.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tk.app.domain.dto.SessionInfo;

// UserDetails : 사용자 인증(Authentication) 정보를 담기 위한 핵심 인터페이스
// 커스텀 UserDetails
public class CustomUserDetails implements UserDetails {
	private static final long serialVersionUID = 1L;
	
	private final SessionInfo memebr;
	private final List<String> roles; // 여러 권한 처리를 위해 리스트로 관리
	private final boolean disabled;
	
	private CustomUserDetails(Builder builder) {
		this.memebr = builder.memebr;
		this.roles = builder.roles;
		this.disabled = builder.disabled;
	}

	// Builder 클래스 정의
	public static class Builder {
		private SessionInfo memebr;
		private List<String> roles;
		private boolean disabled = false;
        
		public Builder sessionInfo(SessionInfo memebr) {
			this.memebr = memebr;
			return this;
		}

		public Builder roles(List<String> roles) {
			this.roles = roles;
			return this;
		}
        
		public Builder disabled(boolean disabled) {
			this.disabled = disabled;
			return this;
		}        

		public CustomUserDetails build() {
			if (this.memebr == null) {
				throw new IllegalStateException("SessionInfo 객체는 필수입니다.");
			}
        	
			return new CustomUserDetails(this);
		 }
	}

	// 빌더 시작 정적 메서드
	public static Builder builder() {
		return new Builder();
	}

	/*
	  - Spring Security 에서 사용자의 roles를 GrantedAuthority 형태로 변환
	    GrantedAuthority 는 사용자가 어떤 권한을 가지고 있는지 Spring Security가 이해할수 있는 형태
	  - Spring Security 에서는 hasRole("ADMIN") 형태로 권한 검사를 할때
	    실제 권한 이름인 ROLE_ADMIN 형태이므로 권한을 ROLE_XXX 로 변환하는 과정
	  - SimpleGrantedAuthority
        : GrantedAuthority의 가장 대표적인 구현체
        : 문자열로 된 권한을 Spring Security가 인식할 수 있는 객체로 감싸준다.
       - roles.stream()
        사용자가 가지고 있는 역할(Role) 문자열들의 컬렉션(List<String>)을 스트림(Stream)으로 변환하여 함수형 스타일로 처리
      - map(...)
        : Stream에 있는 값들을 하나 하나 다른 값으로 매핑한 후, 결과를 스트림으로 반환
        : 각 역할 문자열(role)을 Spring Security가 이해할 수 있는 GrantedAuthority 객체로 변환
      - .collect(Collectors.toList())
        : 필터링, 매핑된 요소들을 새로운 컬렉션에 수집해서 리턴
        : 가공된 SimpleGrantedAuthority 객체들을 다시 List로 수집하여 반환  
	*/	
	// UserDetails 필수 구현 메서드
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return roles.stream()
			.map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
			.collect(Collectors.toList());
	}

	// @Nullable : 해당값이 null 일 수 있음을 명시
	@Override
	public @Nullable String getPassword() {
		return memebr.getPassword();
	}

	@Override
	public String getUsername() { 
		return memebr.getLogin_id();
	}

	@Override 
	public boolean isEnabled() { 
		return !disabled; 
	}
    
	// 스프링 시큐리티 7 : 세션 만료 처리를 위해 hashCode(), equals(Object obj) 재정의
	@Override
	public int hashCode() {
		return Objects.hash(getUsername());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if(obj == null || getClass() != obj.getClass()) return false;
		
		CustomUserDetails that = (CustomUserDetails) obj;
		return Objects.equals(getUsername(), that.getUsername());
	}
	
	public SessionInfo getMember() { 
		return memebr;
	}
	
	// 나머지 설정 디폴트(재정의 하지 않아도 됨)
	@Override public boolean isAccountNonExpired() { return true; }
	@Override public boolean isAccountNonLocked() { return true; }
	@Override public boolean isCredentialsNonExpired() { return true; }

}
