package com.tk.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.mapper.WorkspaceMapper;
import com.tk.app.security.CustomUserDetails;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.WorkChatMemberService;
import com.tk.app.service.WorkspaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/api/workchat")
public class WorkChatMemberController {
	private final WorkChatMemberService chatService;
	private final WorkspaceService workService;
	
	@GetMapping("/members")
	public List<?> getMembers(@PathVariable("domainKey") String domainKey,
			@AuthenticationPrincipal CustomUserDetails userDetails){
		
		SessionInfo member = userDetails.getMember();
	
		if(member == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");	
		}
		
		Long loginMemberId = member.getMember_id();
		
		WorkspaceDto work = workService.findAccessibleWorkspace(domainKey, loginMemberId);
		
		return null;
	}
	
}
