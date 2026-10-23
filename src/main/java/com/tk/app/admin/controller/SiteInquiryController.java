package com.tk.app.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.service.SiteInquiryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
public class SiteInquiryController {
	private final SiteInquiryService siteInquiryService;
	
	
	@GetMapping("/inquiry")
	public String selectAllInquiryList(Model model) {
		try {
			List<SiteInquiryDto> inquiryList = siteInquiryService.selectAllInquiryList();
			
			model.addAttribute("inquiryList", inquiryList);
			model.addAttribute("activeSection", "inquiry");
			model.addAttribute("adminName", "관리자 (ADMIN)");
		} catch (Exception e) {
			log.error("selectAllInquiryList : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/inquiry/{inquiry_id}")
	public String selectAdminInquiryDetail(@PathVariable ("inquiry_id") long inquiry_id,
			Model model) {
		try {
			SiteInquiryDto inquiryDetail = siteInquiryService.selectAdminInquiryDetail(inquiry_id);
			
			if(inquiryDetail == null) {
				return "redirect:/admin/inquiry";
			}
			
			model.addAttribute("inquiryDetail", inquiryDetail);
			model.addAttribute("activeSection", "inquiryDetail");
			model.addAttribute("adminName", "관리자 (ADMIN)");
			
		} catch (Exception e) {
			log.error("selectAdminInquiryDetail : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/inquiry/answer")
	public String updateInquiryAnswer(SiteInquiryDto dto) {
		try {
			siteInquiryService.updateInquiryAnswer(dto);
		} catch (Exception e) {
			log.error("updateInquiryAnswer : ", e);
			throw e;
		}
		return "redirect:/admin/inquiry";
	}
}
