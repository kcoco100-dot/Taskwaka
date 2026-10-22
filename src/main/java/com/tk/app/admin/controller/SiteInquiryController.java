package com.tk.app.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.service.SiteInquiryService;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/service/*")
public class SiteInquiryController {
	private final SiteInquiryService siteInquiryService;
	
	/*
    private Long getLoginMemberId() { //데이터 주인 확인용
        SessionInfo info = LoginMemberUtil.getSessionInfo();
        if (info == null || info.getMember_id() == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        return info.getMember_id();
    } 
		컨트롤러 상단에 선언해서 아래에서 매소드 안에서 
		Long memberId = getLoginMemberId(); 로 호출
	*/
	
	@GetMapping("inquiry")
	public String selectMyInquiryList(Model model) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "redirect:/member/login";
			}
			
			long member_id = info.getMember_id();
			List<SiteInquiryDto> inquiryList = siteInquiryService.selectMyInquiryList(member_id);
			
			model.addAttribute("inquiryList", inquiryList);
			
		} catch (Exception e) {
			log.error("selectMyInquiryList : ", e);
			throw e;
		}
		return "service/inquiry-list";
	}
	
	@GetMapping("inquiry/write")
	public String inquiryWrite() {
		return "service/inquiry-write";
	}
	
	@PostMapping("inquiry/write")
	public String inquiryWriteSubmit(SiteInquiryDto dto) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "redirect:/member/login";
			}
			
			Long member_id =  info.getMember_id();
			dto.setMember_id(member_id);
			
			if(member_id == null) {
				return "redirect:/member/login";
			}
			
			siteInquiryService.insertInquiry(dto);
		} catch (Exception e) {
			log.error("inquiryWriteSubmit : ", e);
			throw e;
		}
		return "redirect:/service/inquiry";
	}
	
}
