package com.tk.app.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestAttribute;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice(assignableTypes = {
        HomeDashboardController.class, HomeTaskController.class,
        HomeGrowthTreeController.class, HomeMeetingMinutesController.class,
        HomeProjectController.class, HomeNoticeController.class,
        Home2Controller.class})
@RequiredArgsConstructor
@Slf4j
public class HomeMenuAdvice {
	private final ProjectMemberRepository projectMemberRepository;

	@ModelAttribute("manageWorkspaceRole")
	public int manageWorkspaceRole(
			@RequestAttribute("workspace") WorkspaceDto workspace) {
		Integer role = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspace.getWorkspaceId(), workspace.getMemberId());
		return role == null ? 0 : role;

	}

}
