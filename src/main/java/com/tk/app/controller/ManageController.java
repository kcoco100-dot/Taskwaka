package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.repository.ProjectMemberRepository;
import com.tk.app.repository.ProjectTeamView;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageController {

	private final ProjectMemberRepository projectMemberRepository;
	
	@Autowired
	private ManageService servive;
	
	@ModelAttribute("domainKey")
	public String setDomainKey(
			@PathVariable("domainKey") String domainKey) {
		
		return domainKey;
	}
	
	// 대시보드 페이지
	@GetMapping
	public String dashBoard() {
		return "manage/pmDashboard";
	}
	
	// 팀원관리 페이지
	@GetMapping("manage")
	public String manage(@PathVariable("domainKey") String domainKey, Model model) {
		try {
			Long workspaceId = servive.findByDomainKey(domainKey);
			
			System.out.println("조회된 워크스페이스 번호: " + workspaceId);
			if(workspaceId != null) {
				List<ProjectTeamView> teamMembers = projectMemberRepository.findWorkspaceMembersWithProject(workspaceId);
				System.out.println("조회된 팀원 수: " + teamMembers.size());
				model.addAttribute("teamMembers", teamMembers);
			}
			
		} catch (Exception e) {
			log.info("manage : ", e);
		}
		
		return "manage/manage";
	}
	
	// 프로젝트 페이지
	@GetMapping("/view")
	public String projectView(Model model) throws Exception {
		
		try {
	        Map<String, Object> map = new HashMap<>();
	        List<ManageDto> projectList =servive.listProject(map);
	        model.addAttribute("projectList", projectList);
		} catch (Exception e) {
			// TODO: handle exception
		}

		
		return "manage/view";
	}
	
	// 프로젝트 등록 페이지
	@GetMapping("/write")
	public String writeForm(Model model) {
		try {
			model.addAttribute("mode", "write");
			model.addAttribute("dto", new ManageDto());
		} catch (Exception e) {
			log.info("writeForm : ", e);
		}
		
		return "manage/viewWrite";
	}
	
	// 프로젝트 등록
	@PostMapping("/write")
	public String writeSubmit(@PathVariable("domainKey") String domainKey,
			ManageDto dto) throws Exception {
		
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) {
				log.info("로그인 정보가 존재하지 않습니다.");
				return "redirect:/login";
			}
			
			Long workspaceId = servive.findByDomainKey(domainKey);
			if (workspaceId == null) {
				throw new IllegalArgumentException("유효하지 않은 워크스페이스 도메인입니다: " + domainKey);
			}
			
			dto.setWorkspace_id(workspaceId);
			dto.setMember_id(info.getMember_id());
			
			dto.setProgress(0);
			
			servive.insertProject(dto);
			
		} catch (Exception e) {
			log.info("writeSubmit : ", e);
		}
		
		return "redirect:/" + domainKey + "/manage/view";
	}
	
	// 캘린더 페이지
	@GetMapping("/calender")
	public String calender() {
		return "manage/manageCalender";
	}
	
}
