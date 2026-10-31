package com.tk.app.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.HomeDashboardDataDto;
import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageNoticeDto;
import com.tk.app.domain.dto.MyTaskDto;
import com.tk.app.domain.dto.ParticipatingProjectDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.domain.dto.WorkspaceMemberGroupDto;
import com.tk.app.domain.dto.WorkspaceMemberGroupDto.ProjectParticipation;
import com.tk.app.repository.ProjectMemberRepository;
import com.tk.app.repository.ProjectTeamView;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageNoticeService;
import com.tk.app.service.ManageService;
import com.tk.app.service.MyTaskService;
import com.tk.app.service.ParticipatingProjectService;

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
	
	private final ParticipatingProjectService participatingProjectService;
	private final MyTaskService myTaskService;
	private final ManageNoticeService manageNoticeService;
	
	// 도메인키 설정
	@ModelAttribute("domainKey")
	public String setDomainKey(
			@PathVariable("domainKey") String domainKey) {

		return domainKey;
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
			Map<Long, Integer> projectStatusMap = new HashMap<>();
			for (ManageDto project : allProjects) {
				if (!Integer.valueOf(2).equals(project.getStatus())) {
					accessibleIds.add(project.getProject_id());
					projectStatusMap.put(project.getProject_id(), project.getStatus());
				}
			}
			
			kwd = kwd == null ? "" : kwd.trim();
            List<ProjectTeamView> rawMembers = projectMemberRepository.findWorkspaceMembersWithProject(workspaceId, kwd);

			Map<Long, WorkspaceMemberGroupDto> groupedMembers = new LinkedHashMap<>();
			Map<Long, String> workspaceRoleNames = new HashMap<>();
			
			for (ProjectTeamView view : rawMembers) {
			    Integer workspaceRole = view.getWorkspaceRole();
			    String roleName = workspaceRole == null ? ""
			            : workspaceRole == 70 ? "사장"
			            : workspaceRole == 50 ? "과장"
			            : workspaceRole == 30 ? "대리"
			            : workspaceRole == 10 ? "사원" : "";

			    workspaceRoleNames.put(view.getWorkspaceMemberId(), roleName);
			
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
					p.setProjectName(view.getProjectName() + (Integer.valueOf(3).equals(projectStatusMap.get(view.getProjectId())) ? " (보류)" : ""));
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
			model.addAttribute("workspaceRoleNames", workspaceRoleNames);
			model.addAttribute("projectList", activeProjects);
			model.addAttribute("currentRole", currentRole);
			model.addAttribute("ownWorkspaceMemberId", ownWorkspaceMemberId);

		} catch (ResponseStatusException e) {
			
			throw e;
			
		} catch (Exception e) {
			log.info("manage : ", e);
            throw new IllegalStateException(e);
		}

		return "manage/Team/manage";
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
            
        } catch (IllegalArgumentException e) {
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
            
        } catch (IllegalStateException e) {
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
            
        } catch (SecurityException e) {
            resultMap.put("status", "fail");
            resultMap.put("message", "로그인 정보 또는 제외 권한을 확인해주세요.");
            
        } catch (Exception e) {
        	
            log.error("excludeProject 오류: ", e);
            resultMap.put("status", "fail");
            resultMap.put("message", "프로젝트 제외 처리 중 오류가 발생했습니다.");
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
        } catch (IllegalArgumentException e) {
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
            
        } catch (IllegalStateException e) {
            resultMap.put("status", "fail");
            resultMap.put("message", e.getMessage());
            
        } catch (SecurityException e) {
            resultMap.put("status", "fail");
            resultMap.put("message", "로그인 정보 또는 제외 권한을 확인해주세요.");
            
        } catch (Exception e) {
            log.error("excludeWorkspace 오류: ", e);
            
            resultMap.put("status", "fail");
            resultMap.put("message", "워크 제외 처리 중 오류가 발생했습니다.");
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
		return "manage/Team/invitation";
	}

    // 캘린더 조회
    @GetMapping("/calender")
    public String calender(@PathVariable("domainKey") String domainKey,
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
        } catch (Exception e) {
            log.info("calender : ", e);
            
            throw e;
        }
        return "manage/calender/manageCalender";
    }

	// 회의록 화면
	@GetMapping("/meetings")
	public String meetingsForm(@PathVariable("domainKey") String domainKey,
				Model model) throws Exception {
		SessionInfo info = LoginMemberUtil.getSessionInfo();
		if (info == null) {
			throw new IllegalStateException("로그인 정보가 없습니다.");
		}
		Long workspaceId = service.findByDomainKey(domainKey);
		if (workspaceId == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
		}
		
		return "manage/meeting/meeting";
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

		return "manage/dashboard/manageGantt";
	}
	
	// 대시보드 화면
	@GetMapping("/pmDashboard")
	public String pmDashboardForm(@PathVariable("domainKey") String domainKey,
			Model model) throws Exception {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			Long workspaceId = service.findByDomainKey(domainKey);
			Map<String, Object> map = new HashMap<>();
			map.put("workspace_id", workspaceId);
			map.put("memberId", info.getMember_id());
			
			List<ManageDto> projectList = service.listProjectManage(map);
			
			int totalProjectCount = projectList.size();
			int plannedProjectCount = 0;
			int inProgressProjectCount = 0;
			int completedProjectCount = 0;
			
			for (ManageDto p : projectList) {
				if (p.getStatus() == 0) {
					plannedProjectCount++;
				} else if (p.getStatus() == 1) {
					inProgressProjectCount++;
				} else if (p.getStatus() == 2) {
					completedProjectCount++;
				}
			}
			
			List<ParticipatingProjectDto> myProjectList = participatingProjectService.findParticipatingProjects(workspaceId, info.getMember_id());
			int myProjectCount = myProjectList.size();
			
			List<MyTaskDto> myTasks = myTaskService.findTasksList(workspaceId, info.getMember_id());
			HomeDashboardDataDto dashboardData = myTaskService.calculateHomeDashboardData(myTasks);
			
			LocalDate today = dashboardData.getToday();
			List<ManageDto> deadlineProjects = new ArrayList<>();

			for (ManageDto project : projectList) {
			    if (project.getStatus() != 0 && project.getStatus() != 1) {
			        continue;
			    }

			    if (project.getEnd_date() == null || project.getEnd_date().isBlank()) {
			        continue;
			    }

			    LocalDate endDate = LocalDate.parse(project.getEnd_date());

			    if (!endDate.isBefore(today)) {
			        deadlineProjects.add(project);
			    }
			}

			deadlineProjects.sort(
			    Comparator.comparing(ManageDto::getEnd_date)
			        .thenComparing(ManageDto::getProject_id)
			);
			
			LocalDate dueSoonDate = today.plusDays(7);
			
			List<ManageDto> dueSoonProjects = new ArrayList<>();

			for (ManageDto project : projectList) {
			    if (project.getStatus() != 0 && project.getStatus() != 1) {
			        continue;
			    }

			    if (project.getEnd_date() == null || project.getEnd_date().isBlank()) {
			        continue;
			    }

			    LocalDate endDate = LocalDate.parse(project.getEnd_date());

			    if (!endDate.isBefore(today) && !endDate.isAfter(dueSoonDate)) {
			        dueSoonProjects.add(project);
			    }
			}

			dueSoonProjects.sort(Comparator.comparing(ManageDto::getEnd_date));
			int dueSoonProjectCount = dueSoonProjects.size();
			List<ManageDto> dueSoonProjectPreview = new ArrayList<>(dueSoonProjects.subList(0, Math.min(2, dueSoonProjects.size())));
			
			List<ManageDto> upcomingProjects = new ArrayList<>();
			for (ManageDto project : projectList) {
				if (project.getStatus() != 0 && project.getStatus() != 1) {
					continue;
				}
				if (project.getEnd_date() == null || project.getEnd_date().isBlank()) {
					continue;
				}
				LocalDate endDate = LocalDate.parse(project.getEnd_date());
				if (!endDate.isBefore(today)) {
					upcomingProjects.add(project);
				}
			}
			upcomingProjects.sort(Comparator.comparing(ManageDto::getEnd_date));
			if (upcomingProjects.size() > 2) {
				upcomingProjects = new ArrayList<>(upcomingProjects.subList(0, 2));
			}
			
			Map<String, Object> noticeMap = new HashMap<>();
			noticeMap.put("domainKey", domainKey);
			noticeMap.put("memberId", info.getMember_id());
			noticeMap.put("noticeType", "all");
			noticeMap.put("schType", "all");
			noticeMap.put("kwd", "");
			noticeMap.put("offset", 0);
			noticeMap.put("size", 5);
			List<ManageNoticeDto> noticeList = manageNoticeService.selectNoticeList(noticeMap);
			
			
			model.addAttribute("projectList", deadlineProjects);
			model.addAttribute("totalProjectCount", totalProjectCount);
			model.addAttribute("plannedProjectCount", plannedProjectCount);
			model.addAttribute("inProgressProjectCount", inProgressProjectCount);
			model.addAttribute("completedProjectCount", completedProjectCount);
			
			model.addAttribute("myProjectList", myProjectList);
			model.addAttribute("myProjectCount", myProjectCount);
			
			model.addAttribute("myTasks", myTasks);
			model.addAttribute("remainingCount", dashboardData.getRemainingCount());
			model.addAttribute("view", dashboardData.getView());
			model.addAttribute("working", dashboardData.getWorking());
			model.addAttribute("preview", dashboardData.getPreview());
			
			model.addAttribute("dueSoonProjectCount", dueSoonProjectCount);
			model.addAttribute("dueSoonProjects", dueSoonProjectPreview);
			model.addAttribute("today", today);
			
			model.addAttribute("upcomingProjects", upcomingProjects);
			
			model.addAttribute("noticeList", noticeList);
			
		} catch (Exception e) {
			log.info("pmDashboardForm : ", e);
			
			throw e;
		}
		return "manage/dashboard/pmDashboard";
		
	}
	
	// 프로젝트 팀원 관리 화면
	@GetMapping("/projectMembers")
	public String projectMembers(@PathVariable("domainKey") String domainKey,
	        @RequestParam(value = "projectId", required = false) Long projectId,
	        Model model) throws Exception {
		
	    try {
	        SessionInfo info = LoginMemberUtil.getSessionInfo();
	        
	        Map<String, Object> data = service.getProjectMemberForm(domainKey, info.getMember_id(), projectId);
	        
	        Long roleWorkspaceId = service.findByDomainKey(domainKey);
	        Map<String, String> workspaceRoleNames = new HashMap<>();

	        List<ProjectTeamView> roleMembers = projectMemberRepository.findWorkspaceMembersWithProject(roleWorkspaceId, "");

	        for (ProjectTeamView view : roleMembers) {
	            Integer workspaceRole = view.getWorkspaceRole();
	            String roleName = workspaceRole == null ? ""
	                    : workspaceRole == 70 ? "사장"
	                    : workspaceRole == 50 ? "과장"
	                    : workspaceRole == 30 ? "대리"
	                    : workspaceRole == 10 ? "사원" : "";

	            workspaceRoleNames.put(String.valueOf(view.getWorkspaceMemberId()), roleName);
	        }

	        model.addAllAttributes(data);
	        
	        model.addAttribute("workspaceRoleNames", workspaceRoleNames);
	        
	    } catch (Exception e) {
	        log.info("projectMembers : ", e);
	        
	        throw e;
	    }
	    return "manage/project/projectTeam";
	}
	
	// 프로젝트 팀원 일괄 저장
	@PostMapping("/projectMembers/save")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> saveProjectMembers(
	        @PathVariable("domainKey") String domainKey,
	        @RequestBody Map<String, Object> map) {
	    Map<String, Object> result = new HashMap<>();
	    try {
	        SessionInfo info = LoginMemberUtil.getSessionInfo();
	        
	        if (map == null) {
	            throw new IllegalArgumentException("저장할 정보가 없습니다.");
	        }
	        
	        int count = service.saveProjectMembers(domainKey, info.getMember_id(), map);
	        result.put("status", "success");
	        result.put("count", count);
	        result.put("message", count + "명의 배정 변경을 저장했습니다.");
	        
	        return ResponseEntity.ok(result);
	        
	    } catch (Exception e) {
	        log.info("saveProjectMembers : ", e);
	        result.put("message", "저장에 실패했습니다. 배정 상태를 다시 확인하세요.");
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
	    }
	}
	
	
	// 프로젝트·업무 자료에 대한 AI 질문
	@PostMapping("/ai/briefing")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> aiBriefing(
	        @PathVariable("domainKey") String domainKey,
	        @RequestBody Map<String, Object> requestData) {

	    ResponseEntity<Map<String, Object>> response;
	    Map<String, Object> result = new HashMap<>();

	    try {
	        SessionInfo info = LoginMemberUtil.getSessionInfo();

	        if (info == null) {
	            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
	        }

	        Object questionValue = requestData.get("question");

	        if (!(questionValue instanceof String)) {
	            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문을 입력해주세요.");
	        }

	        String question = (String) questionValue;

	        String answer = service.createAiBriefing(domainKey, info.getMember_id(), question);
	        
	        result.put("answer", answer);
	        response = ResponseEntity.ok(result);
	        
	    } catch (ResponseStatusException e) {
	    	
	        result.put("message", e.getReason() == null ? "요청을 처리할 수 없습니다." : e.getReason());
	        result.put("code", e.getStatusCode().value() == 429 ? "AI_LIMIT" : "AI_ERROR");

	        response = ResponseEntity.status(e.getStatusCode()).body(result);
	        
	    } catch (Exception e) {
	    	
	        log.info("aiBriefing : ", e);

	        result.put("message", "AI 답변 생성에 실패했습니다. 서버 로그와 API 설정을 확인해주세요.");
	        result.put("code", "AI_ERROR");

	        response = ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
	    }

	    return response;
	}
	
	
}
