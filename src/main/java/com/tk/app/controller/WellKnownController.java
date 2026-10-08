package com.tk.app.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/*
  ※ No static resource .well-known/appspecific/com.chrome.devtools.json 에러
    1. 발생 원인  
       - Google Chrome DevTools가 다음 파일을 자동 탐색
         /.well-known/appspecific/com.chrome.devtools.json
       - Chrome 개발자 도구가 디버깅이나 PWA(Progressive Web App) 관련 정보 또는 특정 인증 정보를 확인하기 위해 사용
       
    2. 해결 방법  
       1) 개발 환경에서만 보이는 경우가 대부분이므로 무시해도 실행에는 문제가 없음
       2) 아래와 같은 빈 컨트롤러 추가를 추가하여 해당 경로를 무시 처리
       3) 정적 파일 직접 제공
         (1) src/main/resources/static/.well-known/appspecific/ 폴더 작성
         (2) 작성된 폴더에 com.chrome.devtools.json 파일 작성후 다음을 입력
             {}          
*/
@Controller
public class WellKnownController {
	@GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
	public ResponseEntity<?> handle(HttpServletRequest req) {
		String redirectUrl = req.getContextPath() + "/";
		return ResponseEntity
				.status(HttpStatus.FOUND)  // 302 상태 코드(리다이렉트)
				.location(URI.create(redirectUrl))  // Location 헤더에 리다이렉트할 URL 설정
				.build();
	}
}

/*
@RestController
public class WellKnownController {
	@GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
	public ResponseEntity<?> chromeDevtools() {
		return ResponseEntity.notFound().build();
	}
}
*/