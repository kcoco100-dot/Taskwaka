package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageTaskDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageProjectController {

	private final ManageProjectService servive;
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;

	// 도메인키 설정
	@ModelAttribute("domainKey")
	public String setDomainKey(
			@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 로그인 회원 조회
	private SessionInfo getLoginInfo() {

		SessionInfo info = LoginMemberUtil.getSessionInfo();

		if (info == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		}

		return info;
	}

	// 조회 조건 생성
	private Map<String, Object> createMap(String domainKey, Long projectId) {

		SessionInfo info = getLoginInfo();
		Map<String, Object> map = new HashMap<>();

		map.put("domainKey", domainKey);
		map.put("projectId", projectId);
		map.put("memberId", info.getMember_id());

		return map;
	}

	// 프로젝트 목록 조회
	@GetMapping("/view")
	public String projectView(
			@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
			@RequestParam(value = "status", defaultValue = "all") String status,
			@RequestParam(name = "schType", defaultValue = "all") String schType,
			@RequestParam(name = "kwd", defaultValue = "") String kwd,
			Model model) throws Exception {

		try {
			SessionInfo info = getLoginInfo();

			int size = 10;
			int dataCount = 0;
			int totalPage = 0;

			status = status == null ? "all" : status.trim();
			schType = schType == null ? "all" : schType.trim();
			kwd = kwd == null ? "" : myUtil.decodeUrl(kwd).trim();

			if (!status.equals("all")
					&& !status.equals("0")
					&& !status.equals("1")
					&& !status.equals("2")
					&& !status.equals("3")) {
				status = "all";
			}

			if (!schType.equals("all")
					&& !schType.equals("subject")
					&& !schType.equals("content")
					&& !schType.equals("manager")) {
				schType = "all";
			}

			Map<String, Object> map = new HashMap<>();

			map.put("domainKey", domainKey);
			map.put("memberId", info.getMember_id());
			map.put("status", status);
			map.put("schType", schType);
			map.put("kwd", kwd);

			dataCount = servive.dataCount(map);
			if (dataCount != 0) {
				totalPage = paginateUtil.pageCount(dataCount, size);
			}
			current_page = Math.min(current_page, totalPage);
			if (current_page < 1) {
				current_page = 1;
			}

			int offset = (current_page - 1) * size;

			map.put("offset", offset);
			map.put("size", size);

			List<ManageDto> projectList = servive.listProject(map);
			String listUrl = RequestUtils.getContextPath() + "/" + domainKey + "/manage/view"
					+ "?status=" + status + "&schType=" + schType + "&kwd=" + myUtil.encodeUrl(kwd);
			String paging = paginateUtil.paging(current_page, totalPage, listUrl);
			model.addAttribute("paging", paging);
			model.addAttribute("pageBaseUrl", listUrl + "&page=");
			Long workspaceId = servive.findByDomainKey(domainKey);
			model.addAttribute("canCreateProject", workspaceId != null && servive.canCreateProject(workspaceId, info.getMember_id()));

			model.addAttribute("projectList", projectList);
			model.addAttribute("dataCount", dataCount);
			model.addAttribute("size", size);
			model.addAttribute("totalPage", totalPage);
			model.addAttribute("page", current_page);

			model.addAttribute("status", status);
			model.addAttribute("schType", schType);
			model.addAttribute("kwd", kwd);

		} catch (Exception e) {
			log.info("projectView : ", e);
			throw e;
		}

		return "manage/view";
	}

	// 프로젝트 등록 화면
	@GetMapping("/write")
	public String writeForm(@PathVariable("domainKey") String domainKey, Model model) throws Exception {
		SessionInfo info = getLoginInfo();
		Long workspaceId = servive.findByDomainKey(domainKey);
		if (workspaceId == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스가 없습니다.");
		}

		model.addAttribute("mode", "write");
		model.addAttribute("dto", new ManageDto());

		return "manage/viewWrite";
	}

	// 프로젝트 등록
	@PostMapping("/write")
	public String writeSubmit(
			@PathVariable("domainKey") String domainKey,
			@ModelAttribute ManageDto dto)
			throws Exception {

		try {
			SessionInfo info = getLoginInfo();

			Long workspaceId = servive.findByDomainKey(domainKey);
			if (workspaceId == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스가 존재하지 않습니다.");
			}

			dto.setWorkspace_id(workspaceId);
			dto.setMember_id(info.getMember_id());
			dto.setProgress(0);

			servive.insertProject(dto);

		} catch (Exception e) {
			log.info("writeSubmit : ", e);
            throw e;
		}

		return "redirect:/" + domainKey + "/manage/view";
	}

	// 프로젝트 상세 조회
	@GetMapping("/detail/{projectId}")
	public String projectDetail(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId,
            @RequestParam(value = "status", defaultValue = "all") String status,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            @RequestParam(value = "page", defaultValue = "1") int page,
			Model model) throws Exception {

		try {
			Map<String, Object> map = createMap(domainKey, projectId);

			ManageDto project = servive.getProjectDetail(map);

			if (project == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
			}

			model.addAttribute("project", project);
			model.addAttribute("currentMemberId", map.get("memberId"));
			model.addAttribute("canEditProject", servive.canManageProject(map));
			map.put("status", status);
            map.put("schType", schType);
            map.put("kwd", kwd.trim());
            model.addAttribute("prev", servive.selectPrevProject(map));
            model.addAttribute("next", servive.selectNextProject(map));
            model.addAttribute("status", status);
            model.addAttribute("schType", schType);
            model.addAttribute("kwd", kwd);
            model.addAttribute("page", page);
            model.addAttribute("taskList",servive.listManageTasks(map));
			model.addAttribute("tlList",servive.listManageTaskTL(map));

		} catch (Exception e) {
			log.info("projectDetail : ", e);
			throw e;
		}
        return "manage/projectDetail";
	}

	// 프로젝트 수정 화면
	@GetMapping("/detail/{projectId}/update")
	public String updateForm(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId,
			Model model) throws Exception {
		try {
			Map<String, Object> map = createMap(domainKey, projectId);
			ManageDto dto = servive.getProjectDetail(map);
			if (dto == null) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
			}
			model.addAttribute("mode", "update");
			model.addAttribute("dto", dto);
			model.addAttribute("projectId", projectId);
		} catch (Exception e) {
			log.info("updateForm : ", e);
			throw e;
		}
        return "manage/viewWrite";
	}

	// 프로젝트 수정
	@PostMapping("/detail/{projectId}/update")
	public String updateSubmit(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId,
			@ModelAttribute ManageDto dto)
			throws Exception {

		try {
			Map<String, Object> map = createMap(domainKey, projectId);

			map.put("subject", dto.getSubject());
			map.put("content", dto.getContent());
			map.put("start_date", dto.getStart_date());
			map.put("end_date", dto.getEnd_date());
			map.put("status", dto.getStatus());

			servive.updateProject(map);

		} catch (Exception e) {
			log.info("updateSubmit : ", e);
            throw e;
		}
        return "redirect:/" + domainKey + "/manage/detail/" + projectId;
	}

	// 프로젝트 숨김
	@PostMapping("/detail/{projectId}/delete")
	public String deleteSubmit(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId)
			throws Exception {

		try {
			Map<String, Object> map = createMap(domainKey, projectId);

			servive.deleteProject(map);

		} catch (Exception e) {
			log.info("deleteSubmit : ", e);
			throw e;
		}
        return "redirect:/" + domainKey + "/manage/view";
	}

	// 업무 등록
	@PostMapping("/detail/{projectId}/tasks/write")
	@ResponseBody
	public Map<String, Object> taskWriteSubmit(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId,
			@ModelAttribute ManageTaskDto dto) {

		Map<String, Object> response = new HashMap<>();

		try {
			Map<String, Object> map = createMap(domainKey, projectId);

			dto.setProject_id(projectId);

			dto.setStatus(0);
			dto.setProgress(0.0);

			servive.insertManageTask(dto, map);

			response.put("status", "success");

		} catch (Exception e) {
			log.info("taskWriteSubmit : ", e);
			response.put("status", "error");
		}

		return response;
	}

	// 업무 수정
	@PostMapping("/detail/{projectId}/tasks/{taskId}/update")
	@ResponseBody
	public Map<String, Object> taskUpdateSubmit(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId,
			@PathVariable("taskId") Long taskId,
			@ModelAttribute ManageTaskDto dto) {

		Map<String, Object> response = new HashMap<>();

		try {
			Map<String, Object> map = createMap(domainKey, projectId);
			map.put("taskId", taskId);

			dto.setProject_id(projectId);
			dto.setTask_id(taskId);

			servive.updateManageTask(dto, map);

			response.put("status", "success");

		} catch (Exception e) {
			log.info("taskUpdateSubmit : ", e);

			response.put("status", "error");
		}

		return response;
	}

	// 업무 숨김
	@PostMapping("/detail/{projectId}/tasks/{taskId}/delete")
	@ResponseBody
	public Map<String, Object> taskDeleteSubmit(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("projectId") Long projectId,
			@PathVariable("taskId") Long taskId) {

		Map<String, Object> response = new HashMap<>();

		try {
			Map<String, Object> map = createMap(domainKey, projectId);
			map.put("taskId", taskId);

			servive.deleteManageTask(map);

			response.put("status", "success");

		} catch (Exception e) {
			log.info("taskDeleteSubmit : ", e);
			response.put("status", "error");
		}

		return response;
	}
}
