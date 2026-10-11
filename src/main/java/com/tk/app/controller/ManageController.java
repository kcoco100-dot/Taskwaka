package com.tk.app.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
			
			SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                return "redirect:/login";
            }
			
            if(workspaceId != null) {
                Integer currentRole = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspaceId, info.getMember_id());
                if (currentRole == null) currentRole = 0;
            
                System.out.println("조회된 워크스페이스 번호: " + workspaceId);
				List<ProjectTeamView> teamMembers = projectMemberRepository.findWorkspaceMembersWithProject(workspaceId);
				System.out.println("조회된 팀원 수: " + teamMembers.size());
				
				Map<String, Object> map = new HashMap<>();
                map.put("workspace_id", workspaceId);
                List<ManageDto> allProjects = servive.listProject(map);
                List<ManageDto> activeProjects = new ArrayList<>();
                java.util.Set<Long> seenProjectIds = new java.util.HashSet<>();
                
                for (ManageDto p : allProjects) {
                    if (p.getStatus() == 0 || p.getStatus() == 1) {
                    	if (!seenProjectIds.contains(p.getProject_id())) {
                            seenProjectIds.add(p.getProject_id());
                            activeProjects.add(p);
                    	}
                    }
                }
                
				model.addAttribute("teamMembers", teamMembers);
				model.addAttribute("projectList", activeProjects);
				model.addAttribute("currentRole", currentRole);
			}
			
		} catch (Exception e) {
			log.info("manage : ", e);
		}
		
		return "manage/manage";
	}
	
	// 인원 프로젝트 배정
	@PostMapping("/assignProject")
    @ResponseBody
    public Map<String, Object> assignProject(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("workspaceMemberId") Long workspaceMemberId,
            @RequestParam("projectId") Long projectId,
            @RequestParam("role") int role) {
            
        Map<String, Object> resultMap = new HashMap<>();
        
        try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) {
			    resultMap.put("status", "fail");
			resultMap.put("message", "로그인이 만료되었거나 정보가 없습니다.");
			    return resultMap;
			}
			
			Long workspaceId = servive.findByDomainKey(domainKey);
			if (workspaceId == null) {
			    resultMap.put("status", "fail");
			resultMap.put("message", "유효하지 않은 워크스페이스입니다.");
			    return resultMap;
			}
			
			Integer currentRole = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspaceId, info.getMember_id());
			if (currentRole == null) {
			    currentRole = 0; // 권한이 아예 없으면 0으로 처리
			}
			
			if (currentRole < 50) {
			    resultMap.put("status", "fail");
			resultMap.put("message", "프로젝트 배정 권한이 없습니다.");
			    return resultMap;
			}
			
			if (currentRole == 50 && role == 70) {
			    resultMap.put("status", "fail");
			resultMap.put("message", "자신보다 높은 권한(PM)을 부여할 수 없습니다.");
			    return resultMap;
			}
        	
        	
            servive.assignOrUpdateProjectMember(workspaceMemberId, projectId, role);
        	
            resultMap.put("status", "success");
            
        } catch (Exception e) {
            log.info("assignProject: ", e);
        }
        
        return resultMap;
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
	
	// 프로젝트 등록 처리
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
