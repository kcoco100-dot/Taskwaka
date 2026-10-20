package com.tk.app.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.tk.app.domain.dto.WorkspaceMemberGroupDto;
import com.tk.app.domain.dto.WorkspaceMemberGroupDto.ProjectParticipation;
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
	private final ManageService servive;
	
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
            
                List<ProjectTeamView> rawMembers = projectMemberRepository.findWorkspaceMembersWithProject(workspaceId);
                
                Map<Long, WorkspaceMemberGroupDto> groupedMembers = new LinkedHashMap<>();
                for (ProjectTeamView view : rawMembers) {
                    
                    WorkspaceMemberGroupDto groupDto = groupedMembers.computeIfAbsent(view.getWorkspaceMemberId(), k -> {
                        WorkspaceMemberGroupDto newDto = new WorkspaceMemberGroupDto();
                        newDto.setWorkspaceMemberId(view.getWorkspaceMemberId());
                        newDto.setName(view.getName());
                        newDto.setStatus(view.getStatus()); 
                        return newDto;
                    });
                    
                    if (view.getProjectId() != null) {
                        ProjectParticipation p = new ProjectParticipation();
                        p.setProjectMemberId(view.getProjectMemberId());
                        p.setProjectId(view.getProjectId());
                        p.setProjectName(view.getProjectName());
                        p.setRole(view.getRole());
                        groupDto.getProjects().add(p);
                    }
                }
                
                List<WorkspaceMemberGroupDto> teamMembers = new ArrayList<>(groupedMembers.values());
                
                Map<String, Object> map = new HashMap<>();
                map.put("workspace_id", workspaceId);
                List<ManageDto> allProjects = servive.listProjectManage(map);
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
	
	 // 프로젝트 인원 등록 및 직위 변경
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
                throw new SecurityException("로그인 정보가 없습니다.");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("actorId", info.getMember_id());
            map.put("workspaceMemberId", workspaceMemberId);
            map.put("projectId", projectId);
            map.put("role", role);

            servive.assignOrUpdateProjectMember(map);

            resultMap.put("status", "success");
            resultMap.put("message", "프로젝트 팀원 배정이 완료되었습니다.");

        } catch (Exception e) {
            log.error("assignProject 오류: ", e);
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
        }

        return resultMap;
    }
	
    // 프로젝트에서 제외
    @PostMapping("/excludeProject")
    @ResponseBody
    public Map<String, Object> excludeProject(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("projectMemberId") Long projectMemberId) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                throw new SecurityException("로그인 정보가 없습니다.");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("actorId", info.getMember_id());
            map.put("projectMemberId", projectMemberId);

            servive.excludeProjectMember(map);

            resultMap.put("status", "success");
            resultMap.put("message", "프로젝트에서 제외되었습니다.");

        } catch (Exception e) {
            log.error("excludeProject 오류: ", e);
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
        }

        return resultMap;
    }

	
    // 워크에서 제외
    @PostMapping("/excludeWorkspace")
    @ResponseBody
    public Map<String, Object> excludeWorkspace(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("workspaceMemberId") Long workspaceMemberId) {

        Map<String, Object> resultMap = new HashMap<>();

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                throw new SecurityException("로그인 정보가 없습니다.");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("actorId", info.getMember_id());
            map.put("workspaceMemberId", workspaceMemberId);

            servive.excludeWorkspaceMember(map);

            resultMap.put("status", "success");
            resultMap.put("message", "워크스페이스에서 제외되었습니다.");

        } catch (Exception e) {
            log.error("excludeWorkspace 오류: ", e);
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
        }

        return resultMap;
    }
	
	// 인원 초대 페이지
	@GetMapping("/invitation")
	public String invitationForm() {
		return "manage/invitation";
	}
	
	// 캘린더 페이지
	@GetMapping("/calender")
	public String calender() {
		return "manage/manageCalender";
	}

	
	// 회의록 페이지
	@GetMapping("/meetings")
	public String meetingsForm() {
		return "manage/meeting";
	}
	
	// 대쉬보드 페이지
	@GetMapping("/pmDashboard")
	public String pmDashboardForm() {
		return "manage/pmDashboard";
	}
	

}
