package com.tk.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.admin.domain.dto.WorkspaceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class Home2Controller {
	
	@ModelAttribute("domainKey")
	public String setDomainKey(
	        @PathVariable("domainKey") String domainKey) {

	    return domainKey;
	}
	
	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(
			@RequestAttribute("workspace") WorkspaceDto workspace) {
		
		return workspace;
	}
	

    // 워크 HOME 화면
    @GetMapping
    public String home() {

        // 기존 HOME 화면 사용
        return "main/home";
    }



    // calendar 페이지
    @GetMapping("/calendar")
    public String calendar() {

        return "main/home";
    }

	
	// manage 페이지
	@GetMapping("/manage")
	public String manage() {
		return "manage/manage";
	}
	
	@GetMapping("/dashboard")
	public String dashboard() {


	    return "main/dashboard";
	}
	
	// projects 페이지
	@GetMapping("/projects")
	public String projects() {
		return "main/projects";
	}
	
	// gantt 페이지
	@GetMapping("/gantt")
	public String gantt() {
		return "home/gantt";
	}
	
	// home - meetings 페이지
	@GetMapping("/meetings")
	public String meetings() {
		return "home/meetings";
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
	@GetMapping({"/manage/view", "/projectView"})
	public String projectView() {
		return "manage/view";
	}

	@GetMapping({"/manage/approval", "/approval"})
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

	
	// 성장나무 화면
	@GetMapping("/growthTree")
	public String growthTree() {
		return "tree/growthTree";
	}
	
	@GetMapping("/manageCalender")
	public String manageCalender() {
	    return "manage/manageCalender";
	}

	@GetMapping("/notice")
	public String notice() {
		return "notice/notice";
	}

	@GetMapping("/noticeDetail")
	public String noticeDetail() {
		return "notice/noticeDetail";
	}

	@GetMapping("/noticeCreate")
	public String noticeCreate() {
		return "notice/noticeCreate";
	}

	@GetMapping("/noticeEdit")
	public String noticeEdit() {
		return "notice/noticeEdit";
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

