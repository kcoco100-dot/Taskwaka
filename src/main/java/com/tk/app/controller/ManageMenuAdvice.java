package com.tk.app.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.HandlerMapping;

import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.repository.ProjectMemberRepository;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice(assignableTypes = {ManageController.class, ManageProjectController.class,
		ManageReportController.class, ManageNoticeController.class, ManageWorkController.class,
		ManageInviteController.class})
@RequiredArgsConstructor
@Slf4j
public class ManageMenuAdvice {
	private final ManageService manageService;
	private final ProjectMemberRepository projectMemberRepository;

	@ModelAttribute("manageWorkspaceRole")
	public int manageWorkspaceRole(HttpServletRequest request) {
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			return 0;
		}
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) {
				return 0;
			}
			Object values = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
			if (!(values instanceof Map<?, ?> variables)) {
				return 0;
			}
			Object value = variables.get("domainKey");
			if (!(value instanceof String domainKey)) {
				return 0;
			}
			Long workspaceId = manageService.findByDomainKey(domainKey);
			if (workspaceId == null) {
				return 0;
			}
			Integer role = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspaceId, info.getMember_id());
			return role == null ? 0 : role;
		} catch (Exception e) {
			log.info("manageWorkspaceRole : ", e);
			return 0;
		}
	}
}
