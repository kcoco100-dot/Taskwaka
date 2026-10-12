package com.tk.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.WorkspaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/myHome") // 워크 선택 전의 화면이기 때문에 도메인 포함X
public class MyHomeController {
	
	private final WorkspaceService workspaceService;
	
	// myHome 에서 워크 정보 목록
	@GetMapping("")
	public String workList(Model model) {
		
		// 회원은 로그인 정보에서 가져오기
		SessionInfo member = LoginMemberUtil.getSessionInfo();
		
		if(member == null) {
			return "redirect:/member/login";
		}
		
		List<WorkspaceDto> workLists = 
				workspaceService.findWorkspaceList(member.getMember_id());
		
		model.addAttribute("workLists", workLists);
		
		return "main/myHome";
	}
}
