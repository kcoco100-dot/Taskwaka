package com.tk.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// 순환 참조를 막기 위해 PasswordEncoder 객체 생성을 별도의 Configuration 클래스로 분리
@Configuration
public class PasswordEncoderConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
