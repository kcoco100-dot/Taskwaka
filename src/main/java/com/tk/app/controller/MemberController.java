package com.tk.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping(value = "/member/*")
public class MemberController {
	
	@GetMapping({"login"})
	public String handleHome(@RequestParam(name = "error", required = false) String error,
			Model model) {
		
		if(error != null) {
			model.addAttribute("message", "아이디 또는 패스워드가 일치하지 않습니다.");
		}
		
		return "member/login-full";
	}
	
	  /**
	  * 3. 회원가입 화면
	   */
	  @GetMapping("/register")
	  public String registerForm() {
	      return "member/register";
	  }

	
	
	@GetMapping("updatePwd")
	public String updatePwdForm() throws Exception{
		return "member/updatePwd";
	}

	@GetMapping("expired")
	public String expired() throws Exception {
		// 세션이 익스파이어드(만료) 된 경우
		return "member/expired";
	}	
}
