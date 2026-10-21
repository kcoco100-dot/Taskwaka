
package com.tk.app.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.ManageWorkDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageService;
import com.tk.app.service.ManageWorkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageWorkController {
	private final ManageService manageService;
	private final ManageWorkService service;
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;

	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {
		String result = domainKey;
		return result;
	}

	// 워크 팀원 직위 목록
	@GetMapping("/workspaceRoles")
	public String listWorkMember(@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
			@RequestParam(value = "keyword", defaultValue = "") String keyword,
			Model model) throws Exception {
		String result = "manage/workspaceRoles";
		SessionInfo info = LoginMemberUtil.getSessionInfo();

		if (info == null) {
			result = "redirect:/login";
		} else {
			try {
				Long workspaceId = manageService.findByDomainKey(domainKey);
				if (workspaceId == null) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
				}

				keyword = keyword == null ? "" : keyword.trim();

				int size = 10;
				int dataCount = 0;
				int totalPage = 0;
				List<ManageWorkDto> memberList = new ArrayList<>();

				dataCount = service.dataCount(workspaceId, info.getMember_id(), keyword);
				if (dataCount != 0) {
					totalPage = paginateUtil.pageCount(dataCount, size);
				}

				current_page = Math.min(current_page, totalPage);
				if (current_page < 1) current_page = 1;

				memberList = service.listWorkMember(
					workspaceId, info.getMember_id(), keyword, current_page, size
				);

				String listUrl = RequestUtils.getContextPath() + "/" + domainKey + "/manage/workspaceRoles?keyword=" + myUtil.encodeUrl(keyword);
				String paging = paginateUtil.paging(current_page, totalPage, listUrl);

				model.addAttribute("memberList", memberList);
				model.addAttribute("keyword", keyword);
				model.addAttribute("page", current_page);
				model.addAttribute("dataCount", dataCount);
				model.addAttribute("totalPage", totalPage);
				model.addAttribute("paging", paging);
				model.addAttribute("pageBaseUrl", listUrl + "&page=");
			} catch (SecurityException e) {
				log.info("listWorkMember : ", e);
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
			} catch (Exception e) {
				log.info("listWorkMember : ", e);
				throw e;
			}
		}

		return result;
	}

	// 워크 팀원 직위 변경
	@PostMapping("/workspaceRoles/update")
	public String updateWorkRole(@PathVariable("domainKey") String domainKey,
			@RequestParam("workspaceMemberId") Long workspaceMemberId,
			@RequestParam("role") int role,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "keyword", defaultValue = "") String keyword,
			RedirectAttributes redirectAttributes) throws Exception {
		String result = "redirect:/{domainKey}/manage/workspaceRoles";
		SessionInfo info = LoginMemberUtil.getSessionInfo();

		if (info == null) {
			result = "redirect:/login";
		} else {
			try {
				Long workspaceId = manageService.findByDomainKey(domainKey);
				if (workspaceId == null) {
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
				}

				service.updateWorkRole(
					workspaceId, info.getMember_id(), workspaceMemberId, role
				);

				redirectAttributes.addFlashAttribute("message", "워크스페이스 직위를 변경했습니다.");
			} catch (SecurityException e) {
				log.info("updateWorkRole : ", e);
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
			} catch (IllegalArgumentException e) {
				log.info("updateWorkRole : ", e);
				redirectAttributes.addFlashAttribute("error", e.getMessage());
			} catch (Exception e) {
				log.info("updateWorkRole : ", e);
				throw e;
			}

			redirectAttributes.addAttribute("domainKey", domainKey);
			redirectAttributes.addAttribute("page", page);
			redirectAttributes.addAttribute("keyword", keyword);
		}

		return result;
	}
}
