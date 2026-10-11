package com.tk.app.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.common.PaginateUtil;
import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.domain.dto.MyTaskDto;
import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.domain.dto.ParticipatingProjectDto;
import com.tk.app.service.MeetingMinutesService;
import com.tk.app.service.MyTaskService;
import com.tk.app.service.NoticeService;
import com.tk.app.service.ParticipatingProjectService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class Home2Controller {
	
	private final ParticipatingProjectService participatingProjectService;
	private final NoticeService noticeService;
	private final MeetingMinutesService meetingMinutesService;
	private final PaginateUtil paginateUtil;
	private final MyTaskService myTaskService;
	
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

    // 담당 업무 기간 및 진행률 Home 캘린더에 표시
    @GetMapping("/calendar")
    public String calendar(
    		@RequestAttribute("workspace") WorkspaceDto workspace,
    		Model model) {
    	
    		List<MyTaskDto> myTasks = myTaskService.findTasksList(
    				workspace.getWorkspaceId(),
    				workspace.getMemberId());
    		
    		model.addAttribute("myTasks", myTasks);

        return "main/home";
    }

	
	// manage 페이지
	@GetMapping("/manage")
	public String manage() {
		return "manage/manage";
	}
	
	// 기존 home 화면, 내 업무 목록 조회
	@GetMapping({"", "/dashboard"})
	public String dashboard(
			@RequestAttribute("workspace") WorkspaceDto worksapce,
			Model model) {

		List<MyTaskDto> myTasks = myTaskService.findTasksList(
				worksapce.getWorkspaceId(), 
				worksapce.getMemberId());
		
		model.addAttribute("myTasks", myTasks);

	    return "main/dashboard";
	}
	
	// projects 페이지
	@GetMapping("/projects")
	public String projects(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			Model model) {
		
		// 프로젝트 목록 조회
		List<ParticipatingProjectDto> participatingProjects = 
				participatingProjectService.findParticipatingProjects(
						workspace.getWorkspaceId(),
						workspace.getMemberId());
		
		// 조회한 데이터를 담아서 HTML 에 보낸다.
		model.addAttribute("participatingProjects", participatingProjects);
		
		return "main/projects";
	}
	
	// gantt 페이지
	@GetMapping("/gantt")
	public String gantt() {
		return "home/gantt";
	}
	
	// home - meetings 목록 페이지
	@GetMapping("/meetings")
	public String meetings(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@RequestParam(value = "kwd", defaultValue = "") String kwd,
			@RequestParam(value = "page", defaultValue = "1") int page,
			HttpServletRequest request,
			Model model) {
		
		int size = 10;
		
		// 검색 조건에 맞는 건수 & 페이지 수
		int dataCount = meetingMinutesService.countMeetingMinutes(
				workspace.getWorkspaceId(), workspace.getMemberId(),
				kwd);
		
		int totalPage = paginateUtil.pageCount(dataCount, size);
		
		// 만약 페이지가 1보다 작거나 전체 페이지가 0이면 1로 맞춰주기
		if(page < 1 || totalPage == 0) {
			page = 1;
		} else if(page > totalPage) {
			page = totalPage;
		}
		
		int offset = (page - 1) * size;
		
		// 회의록 목록 조회
		List<MeetingMinutesDto> meetingMinutes = 
				meetingMinutesService.findMeetingMinutes(
						workspace.getWorkspaceId(),
						workspace.getMemberId(), kwd, offset, size);
		
		// 현재 워크 목록 주소 유지
		// getRequestURI : 현재 도메인이 포함된 목록 경로 가져오기
		String listUrl = request.getRequestURI();
		
		// 페이지 이동 시 검색어 유지하기
		if(!kwd.isEmpty()) {
			listUrl += "?kwd=" + URLEncoder.encode(kwd,StandardCharsets.UTF_8);
		}
		
		// 페이지 번호와 이동 링크가 들어있는 HTML을 만듦
		// htmlEscape : 특수 문자 변환 (& -> &amp; 로 변경) 
		String paging = paginateUtil.paging(page, totalPage, HtmlUtils.htmlEscape(listUrl));
		
		model.addAttribute("paging", paging);
		model.addAttribute("kwd", kwd);
		model.addAttribute("meetingMinutes", meetingMinutes);
		
		return "home/meetings";
	}
	
	// home - meetingsDetail 페이지
	@GetMapping("/meetingsDetail")
	public String meetingsDetail(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@RequestParam("meetingMinuteId") long meetingMinuteId,
			Model model) throws Exception {
		
		MeetingMinutesDto meeting = meetingMinutesService.findMeetingDetailMinutes(
				workspace.getWorkspaceId(), workspace.getMemberId(), meetingMinuteId);
		
		if(meeting == null) {
			throw new Exception("회의록을 찾을 수 없습니다.");
		}
		
		model.addAttribute("meeting", meeting);
		
		return "home/meetingsDetail";
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
	public String notice(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			Model model) {
		
		List<NoticeDto> notices = noticeService.findWorkspaceNotice(workspace.getWorkspaceId());
		
		model.addAttribute("notices", notices);
		
		return "notice/notice";
	}

	@GetMapping("/noticeDetail")
	public String noticeDetail(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@RequestParam("noticeId") long noticeId,
			Model model) throws Exception {
		
		NoticeDto notice = noticeService.findWorkspaceDetailNotice(
				workspace.getWorkspaceId(), noticeId);
		
		if(notice == null) {
			throw new Exception("공지를 찾을 수 없습니다.");
		}
		
		model.addAttribute("notice", notice);
		
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

