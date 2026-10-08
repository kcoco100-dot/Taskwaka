package com.tk.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

/*
  ## SecurityFilterChain
    - http 요청에 대하여 어떤 보안 필터들을 어떤 순서로 적용할지 정의하는 객체
    - http 요청 -> 여러 Security Filter -> Controller
    - return http.build()
      HttpSecurity 로 보안 설정을 하고, build() 를 호출하면 설정을 기반으로
      SecurityFilterChain 객체가 생성된다.

  ## HttpSecurity
    - http 요청에 대한 보안 설정을 구성하는 핵심 객체
      어떤 요청을 누구에게 허용하고, 어떤 인증 방식을 사용할 지 설정
    - authorizeHttpRequests() : URL 별 접근 권한 설정
    - requestMatchers() : 특정 URL을 지정
    - permitAll() : 인증 없이 접근 허용
    - authenticated() : 로그인한 자용자만 허용
    - formLogin() : 폼 로그인 설정
    - csrf() : CSRF 보안 설정
    - sessionManagement() : 세션 정책 설정
    - logout() : 로그아웃 설정
	
  ## url?continue
    : 주소 뒤에 ?continue 붙는 경우가 발생
    : 스프링 시큐리티가 6.x로 업그레이드되면서 스프링부트 안정성을 추구하면서 발생되는 현상
    
  ## CORS(Cross-Origin Resource Sharing)
    : 교차 출처 리소스 공유라는 의미
    : 웹 브라우저에서 다른 출처(origin)의 리소스에 접근할 수 있도록 허용하는 메커니즘
    
  ## CSRF(Cross-Site Request Forgery, 사이트 간 요청 위조)
    : 교차 사이트 요청 위조
    : 사용자가 로그인한 상태를 악용해서, 공격자가 사용자의 의도와 관계없이 해당 사이트에 요청을 보내게 만드는 공격
    : 스프링 시큐리티는 기본적으로 웹 브라우저 기반의 요청에서 이러한 공격을 방어하기 위해 
      모든 상태 변경 요청(POST, PUT, DELETE 등)에 대해 CSRF 토큰(랜덤 값)을 검증한다.
    
  ## authorizeHttpRequests()
    : 스프링 시큐리티의 구성 메서드 내에서 사용되는 메서드로, HTTP 요청에 대한 인가 설정을 구성하는 데 사용
    : 다양한 인가 규칙을 정의할 수 있으며, 경로별로 다른 권한 설정이 가능하다.
    	    
  ## requestMatchers()
    : authorizeHttpRequests()와 함께 사용되어 특정한 HTTP 요청 매처(Request Matcher)를 적용할 수 있게 해준다.
    : 요청의 종류는 HTTP 메서드(GET, POST 등)나 서블릿 경로를 기반으로 지정할 수 있다.
      requestMatchers(HttpMethod.GET,  "/public/**") 처럼 
      HTTP GET 요청 중 "/public/"으로 시작하는 URL에 대한 보안 설정  

  ## UserDetailsService
    : JDBC 연동은 UserDetailsService 구현 클래스 작성
    : 스프링 시큐리티에서 사용자 인증을 처리할 때 사용되는 인터페이스
    : 주로 사용자 정보를 데이터베이스나 다른 저장소에서 조회하여 인증 및 권한 부여에 필요한 사용자 정보를 제공하는 역할
    : 스프링 시큐리티는 UserDetailsService를 통해 사용자가 로그인할 때 필요한 
      사용자 정보(Username, Password, 권한 등)을 UserDetails 객체로 반환하여 인증 처리 및 권한 검증을 진행       
*/

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SpringSecurityConfig {

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
				// 1. 주소별 로그인 권한 설정
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/member/login", "/member/register", "/member/account", "/member/pwdFind",
								"/member/expired", "/dist/**", "/uploads/**", "/css/**", "/js/**", "/images/**",
								"/favicon.ico", "/error/**")
						.permitAll()

						// 관리자만 접근
						.requestMatchers("/admin", "/admin/**").hasRole("ADMIN")

						// 그 외 주소는 로그인 필요
						.anyRequest().authenticated())

				// 2. 기존 로그인 화면 연결
				.formLogin(form -> form.loginPage("/member/login").loginProcessingUrl("/member/login")
						.usernameParameter("login_id").passwordParameter("password").defaultSuccessUrl("/main", true)
						.failureUrl("/member/login?error").permitAll())

				// 3. 로그아웃 설정
				.logout(logout -> logout.logoutUrl("/member/logout").logoutSuccessUrl("/member/login"))

				// 기존 프로젝트 설정 유지
				.csrf(AbstractHttpConfigurer::disable);

		return http.build();
	}

}
