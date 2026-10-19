package com.tk.app.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.admin.domain.dto.SiteNoticeDto;
import com.tk.app.admin.service.SiteNoticeService;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
public class SiteNoticeController {
	private final SiteNoticeService siteNoticeService;

	@GetMapping("/notice")
	public String noticeList(Model model) {
		try {
			List<SiteNoticeDto> noticeList = siteNoticeService.selectNoticeList();
			
			model.addAttribute("noticeList", noticeList);
			model.addAttribute("activeSection", "notice");
	        model.addAttribute("adminName", "관리자 (ADMIN)");
	        
		} catch (Exception e) {
			log.error("notice 오류 : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/notice")
	public String insertNotice(SiteNoticeDto dto) {
		try {
			SessionInfo sessionInfo = LoginMemberUtil.getSessionInfo();
			
			if(sessionInfo == null) {
				return "redirect:/member/login";
			}
			
			dto.setMember_id(sessionInfo.getMember_id());
			
			siteNoticeService.insertNotice(dto);
			
		} catch (Exception e) {
			log.error("insertNotice 오류 : ", e);
			throw e;
		}
		return "redirect:/admin/notice";
	}
	
	@GetMapping("/notice/{notice_id}")
	public String noticeDetail(@PathVariable long notice_id,
			Model model){
		
		try {
			SiteNoticeDto dto = siteNoticeService.selectNoticeDetail(notice_id);
			
			model.addAttribute("detail", dto);
			model.addAttribute("activeSection", "noticeDetail");
	        model.addAttribute("adminName", "관리자 (ADMIN)");
			
		} catch (Exception e) {
			log.error("noticeDetail 오류 : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/notice/update")
	public String updateNotice(SiteNoticeDto dto,
			@RequestParam(name = "hideNotice", defaultValue = "false") boolean hideNotice) {
		
		try {
			dto.setStatus(hideNotice ? 0 : 1);
			
			siteNoticeService.updateNotice(dto);
		} catch (Exception e) {
			log.error("updateNotice 오류 : ", e);
			throw e;
		}
		
		return "redirect:/admin/notice";
	}
	
}
