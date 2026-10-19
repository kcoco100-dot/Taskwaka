package com.tk.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.security.CustomUserDetails;
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
	public ResponseEntity<?> getMembers(@PathVariable("domainKey") String domainKey,
			@AuthenticationPrincipal CustomUserDetails userDetails){
		
		if(userDetails == null || userDetails.getMember() == null) {
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.build();
		}
		
		SessionInfo member = userDetails.getMember();
		Long loginMemberId = member.getMember_id();
		
		WorkspaceDto work = workService.findAccessibleWorkspace(domainKey, loginMemberId);
		
		if(work == null) {
			return ResponseEntity
					.status(HttpStatus.FORBIDDEN)
					.build();
		}
		
		Long workId = work.getWorkspaceId();
		
		List<WorkChatMemberDto> members = chatService.getMembers(workId, loginMemberId);
		
		return ResponseEntity.ok(members);
	}
	
}
