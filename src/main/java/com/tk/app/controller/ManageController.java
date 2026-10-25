package com.tk.app.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tk.app.common.PaginateUtil;
import com.tk.app.common.MyUtil;
import com.tk.app.common.RequestUtils;
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
	private final ManageService service;
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;

	// 도메인키 설정
	@ModelAttribute("domainKey")
	public String setDomainKey(
			@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 대시보드 화면
	@GetMapping
	public String dashBoard(@PathVariable("domainKey") String domainKey) throws Exception {
		return "manage/pmDashboard";
	}

	// 팀원 목록 조회
	@GetMapping("manage")
	public String manage(@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd, Model model) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) {
				throw new IllegalStateException("로그인 정보가 없습니다.");
			}
			Long workspaceId = service.findByDomainKey(domainKey);
			if (workspaceId == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
			}
			Integer currentRole = projectMemberRepository.findRoleByWorkspaceIdAndMemberId(workspaceId, info.getMember_id());
            if (currentRole == null) {
                currentRole = 0;
            }

			Map<String, Object> map = new HashMap<>();
			map.put("workspace_id", workspaceId);
			map.put("actorId", info.getMember_id());
			map.put("workspaceId", workspaceId);
			Long ownWorkspaceMemberId = currentRole == 70 ? service.getOwnWorkspaceMemberId(map) : null;
			List<ManageDto> allProjects = service.listProjectManage(map);
			java.util.Set<Long> accessibleIds = new java.util.HashSet<>();
			for (ManageDto project : allProjects) {
				accessibleIds.add(project.getProject_id());
			}
			kwd = kwd == null ? "" : kwd.trim();
            List<ProjectTeamView> rawMembers = projectMemberRepository.findWorkspaceMembersWithProject(workspaceId, kwd);

			Map<Long, WorkspaceMemberGroupDto> groupedMembers = new LinkedHashMap<>();
			for (ProjectTeamView view : rawMembers) {
				
				WorkspaceMemberGroupDto groupDto = groupedMembers.computeIfAbsent(view.getWorkspaceMemberId(), k -> {
					WorkspaceMemberGroupDto newDto = new WorkspaceMemberGroupDto();
					newDto.setWorkspaceMemberId(view.getWorkspaceMemberId());
					newDto.setName(view.getName());
					newDto.setStatus(view.getStatus());
					return newDto;
				});

				if (view.getProjectId() != null && accessibleIds.contains(view.getProjectId())) {
					ProjectParticipation p = new ProjectParticipation();
					p.setProjectMemberId(view.getProjectMemberId());
					p.setProjectId(view.getProjectId());
					p.setProjectName(view.getProjectName());
					p.setRole(view.getRole());
					groupDto.getProjects().add(p);
				}
			}

			List<WorkspaceMemberGroupDto> allMembers = new ArrayList<>(groupedMembers.values());
			int size = 10;
			int dataCount = 0;
			int totalPage = 0;
			dataCount = allMembers.size();
			if (dataCount != 0) {
				totalPage = paginateUtil.pageCount(dataCount, size);
			}
			current_page = Math.min(current_page, totalPage);
			if (current_page < 1) {
				current_page = 1;
			}
			int offset = (current_page - 1) * size;
			int end = Math.min(offset + size, dataCount);
			List<WorkspaceMemberGroupDto> teamMembers = allMembers.subList(offset, end);
			String listUrl = RequestUtils.getContextPath() + "/" + domainKey + "/manage/manage?schType=" + myUtil.encodeUrl(schType) + "&kwd=" + myUtil.encodeUrl(kwd);
            model.addAttribute("schType", schType);
            model.addAttribute("kwd", kwd);
			String paging = paginateUtil.paging(current_page, totalPage, listUrl);
			model.addAttribute("paging", paging);
			model.addAttribute("pageBaseUrl", listUrl + "&page=");
			model.addAttribute("page", current_page);
			model.addAttribute("size", size);
			model.addAttribute("dataCount", dataCount);
			model.addAttribute("totalPage", totalPage);

			List<ManageDto> activeProjects = new ArrayList<>();
			for (ManageDto p : allProjects) {
				if (p.getStatus() == 0 || p.getStatus() == 1) {
					activeProjects.add(p);
				}
			}

			model.addAttribute("teamMembers", teamMembers);
			model.addAttribute("projectList", activeProjects);
			model.addAttribute("currentRole", currentRole);
			model.addAttribute("ownWorkspaceMemberId", ownWorkspaceMemberId);

		} catch (ResponseStatusException e) {
			throw e;
		} catch (Exception e) {
			log.info("manage : ", e);
            throw new IllegalStateException(e);
		}

		return "manage/manage";
	}

	// 프로젝트 배정 및 직위 변경
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

			service.assignOrUpdateProjectMember(map);

			resultMap.put("status", "success");

		} catch (Exception e) {
			log.error("assignProject 오류: ", e);
			resultMap.put("status", "fail");
		}

		return resultMap;
	}

	// 프로젝트 팀원 제외
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

			service.excludeProjectMember(map);

			resultMap.put("status", "success");

		} catch (Exception e) {
			log.error("excludeProject 오류: ", e);
			resultMap.put("status", "fail");
		}

		return resultMap;
	}

	// 워크 팀원 제외
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

			service.excludeWorkspaceMember(map);

			resultMap.put("status", "success");

		} catch (Exception e) {
			log.error("excludeWorkspace 오류: ", e);
			resultMap.put("status", "fail");
		}

		return resultMap;
	}

	// 팀원 초대 화면
	@GetMapping("/invitation")
	public String invitationForm(@PathVariable("domainKey") String domainKey) throws Exception {
		SessionInfo info = LoginMemberUtil.getSessionInfo();
		if (info == null) {
			throw new IllegalStateException("로그인 정보가 없습니다.");
		}
		Long workspaceId = service.findByDomainKey(domainKey);
		return "manage/invitation";
	}

    // 캘린더 조회
    @GetMapping("/calender")
    public String calender(@PathVariable("domainKey") String domainKey, Model model) throws Exception {
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            Long workspaceId = service.findByDomainKey(domainKey);
            Map<String, Object> map = new HashMap<>();
            map.put("workspaceId", workspaceId);
            map.put("memberId", info.getMember_id());
            Map<String, Object> calendarData = service.getManageCalendarData(map);
            model.addAttribute("myProjects", calendarData.get("myProjects"));
            model.addAttribute("myTasks", calendarData.get("myTasks"));
        } catch (Exception e) {
            log.info("calender : ", e);
            throw e;
        }
        return "manage/manageCalender";
    }

	// 회의록 화면
	@GetMapping("/meetings")
	public String meetingsForm(@PathVariable("domainKey") String domainKey) throws Exception {
		SessionInfo info = LoginMemberUtil.getSessionInfo();
		if (info == null) {
			throw new IllegalStateException("로그인 정보가 없습니다.");
		}
		Long workspaceId = service.findByDomainKey(domainKey);
		if (workspaceId == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
		}
		return "manage/meeting";
	}

	// 대시보드 화면
	@GetMapping("/pmDashboard")
	public String pmDashboardForm(@PathVariable("domainKey") String domainKey) throws Exception {
		SessionInfo info = LoginMemberUtil.getSessionInfo();
		if (info == null) {
			throw new IllegalStateException("로그인 정보가 없습니다.");
		}
		Long workspaceId = service.findByDomainKey(domainKey);
		if (workspaceId == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
		}
		return "manage/pmDashboard";
	}

	// 간트차트 화면
	@GetMapping("/gantt")
	public String gantt(
			@PathVariable("domainKey") String domainKey,
			@RequestParam(name = "schType", defaultValue = "all") String schType,
			@RequestParam(name = "kwd", defaultValue = "") String kwd,
			Model model) throws Exception {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			Long workspaceId = service.findByDomainKey(domainKey);
			Map<String, Object> map = new HashMap<>();
			map.put("workspaceId", workspaceId);
			map.put("memberId", info.getMember_id());
			Map<String, Object> calendarData = service.getManageCalendarData(map);

			model.addAttribute("myProjects", calendarData.get("myProjects"));
			model.addAttribute("myTasks", calendarData.get("myTasks"));
			model.addAttribute("schType", schType);
			model.addAttribute("kwd", kwd == null ? "" : kwd.trim());
		} catch (Exception e) {
			log.info("gantt : ", e);
			throw e;
		}

		return "manage/manageGantt";
	}
}
