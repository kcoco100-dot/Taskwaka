package com.tk.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.admin.domain.dto.WorkspaceDto;

@Controller
@RequestMapping("/{domainKey}/home")
public class Home2Controller {

	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}
	
	// manage 페이지
	@GetMapping("/manage")
	public String manage() {
		return "manage/manage";
	}

	// manage - meetings 페이지
	@GetMapping("/managemeetings")
	public String managemeetings() {
		return "manage/meetings";
	}

	// box 페이지
	@GetMapping("/documentBox")
	public String documentBox() {
		return "box/documentBox";
	}

	// mail 페이지
	@GetMapping("/mail")
	public String mail() {
		return "mail/mail";
	}

	// invitation 페이지
	@GetMapping("/invitation")
	public String invitation() {
		return "team/invitation";
	}

	// projectCreate 페이지
	@GetMapping("/projectCreate")
	public String projectCreate() {
		return "project/create";
	}

	// projectView 페이지
	@GetMapping({ "/manage/view", "/projectView" })
	public String projectView() {
		return "manage/view";
	}

	@GetMapping({ "/manage/approval", "/approval" })
	public String approval() {
		return "manage/approval";
	}

	@GetMapping("/pmDashboard")
	public String pmDashboard() {
		return "manage/pmDashboard";
	}

	@GetMapping("/manage/meetings")
	public String manageMeetings() {
		return "manage/meetings";
	}

	@GetMapping("/manageCalender")
	public String manageCalender() {
		return "manage/manageCalender";
	}

	@GetMapping("/plan")
	public String plan() {
		return "plan/plan";
	}

	@GetMapping("/planPayment")
	public String planPayment() {
		return "plan/planPayment";
	}
}
