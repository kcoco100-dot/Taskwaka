package com.tk.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.OrganizationModalDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.OrganizationModalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/api/organization")
public class OrganizationModalController {

	private final OrganizationModalService service;

	private SessionInfo getLoginMember() {
		SessionInfo member = LoginMemberUtil.getSessionInfo();

		if(member == null || member.getMember_id() == null) {
			throw new ResponseStatusException(
					HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		return member;
	}

	private OrganizationModalDto getQuery(String domainKey) {
		SessionInfo member = getLoginMember();

		OrganizationModalDto dto = new OrganizationModalDto();
		dto.setDomainKey(domainKey);
		dto.setMemberId(member.getMember_id());

		return dto;
	}

	@GetMapping("/projects")
	public ResponseEntity<?> findMyProjects(
			@PathVariable("domainKey") String domainKey) {

		OrganizationModalDto dto = getQuery(domainKey);
		return ResponseEntity.ok(service.findMyProjects(dto));
	}

	@GetMapping("/projects/{projectId}/tree")
	public ResponseEntity<?> findProjectTree(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId) {

		OrganizationModalDto dto = getQuery(domainKey);
		dto.setProjectId(projectId);

		return ResponseEntity.ok(service.findProjectTree(dto));
	}
}
