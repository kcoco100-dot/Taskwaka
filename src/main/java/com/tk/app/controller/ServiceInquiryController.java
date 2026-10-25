package com.tk.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.domain.dto.SiteInquiryFileDto;
import com.tk.app.admin.service.SiteInquiryService;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/service/*")
public class ServiceInquiryController {
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
	public String inquiryWriteSubmit(SiteInquiryDto dto,
			@RequestParam(name = "files", required = false) MultipartFile[] files) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "redirect:/member/login";
			}
			
			Long member_id =  info.getMember_id();

			dto.setMember_id(member_id);
			
			siteInquiryService.insertInquiry(dto, files);
		} catch (Exception e) {
			log.error("inquiryWriteSubmit : ", e);
			throw e;
		}
		return "redirect:/service/inquiry";
	}
	
	@GetMapping("inquiry/{inquiry_id}")
	public String selectInquiryDetail(@PathVariable ("inquiry_id") long inquiry_id,
			RedirectAttributes redirectAttributes,
			Model model) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if(info == null) {
				return "redirect:/member/login";
			}
			
			long member_id = info.getMember_id();
			
			SiteInquiryDto inquiry = siteInquiryService.selectInquiryDetail(inquiry_id, member_id);
			if(inquiry == null) {
				redirectAttributes.addFlashAttribute("message", "존재하지 않거나 접근 권한이 없는 문의입니다.");
				return "redirect:/service/inquiry";
			}
			
			List<SiteInquiryFileDto> fileList = siteInquiryService.selectInquiryFileList(inquiry_id);
			
			model.addAttribute("fileList", fileList);
			model.addAttribute("inquiry", inquiry);
			
		} catch (Exception e) {
			log.error("selectInquiryDetail : ", e);
			throw e;
		}
		return "service/inquiry-detail";
	}
	
	@GetMapping("inquiry/{inquiry_id}/edit")
	public String inquiryEdit(@PathVariable ("inquiry_id") long inquiry_id,
			Model model) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "redirect:/member/login";
			}
			
			long member_id = info.getMember_id();
			
			SiteInquiryDto inquiry = siteInquiryService.selectInquiryDetail(inquiry_id, member_id);
			
			boolean canEdit = siteInquiryService.canEditInquiry(inquiry_id, member_id);
			
			if(!canEdit) {
				return "redirect:/service/inquiry";
			}
			List<SiteInquiryFileDto> fileList = siteInquiryService.selectInquiryFileList(inquiry_id);
			
			model.addAttribute("fileList", fileList);
			model.addAttribute("inquiry", inquiry);
			
		} catch (Exception e) {
			log.error("inquiryEdit : ", e);
			throw e;
		}
		return "service/inquiry-edit";
	}
	
	@PostMapping("inquiry/{inquiry_id}/edit")
	@ResponseBody
	public String inquiryEditSubmit(@PathVariable("inquiry_id") long inquiry_id, SiteInquiryDto dto,
			@RequestParam(name="files", required=false) MultipartFile[] files) {
	    try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "login";
			}
			
			dto.setInquiry_id(inquiry_id);
			dto.setMember_id(info.getMember_id());
			
			int result = siteInquiryService.updateInquiry(dto, files);
			
			if(result == 0) {
				return "fail";
			}
			
		} catch (Exception e) {
			log.error("inquiryEdit : ", e);
			throw e;
		}
		
	    return "success";
	}
	
	@PostMapping("inquiry/{inquiry_id}/delete")
	@ResponseBody
	public String deleteInquiry(@PathVariable ("inquiry_id") long inquiry_id) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "login";
			}
			
			SiteInquiryDto dto = new SiteInquiryDto();
			
			dto.setInquiry_id(inquiry_id);
			dto.setMember_id(info.getMember_id());
			
			int result = siteInquiryService.deleteInquiry(dto);
			
			if(result == 0) {
				return "fail";
			}
		
		} catch (Exception e) {
			log.error("deleteInquiry : ", e);
			throw e;
		}
		return "success";
	}
	
	@PostMapping("inquiry/file/{inquiries_file_id}/delete")
	@ResponseBody
	public String deleteInquiryFile(@PathVariable ("inquiries_file_id") long inquiries_file_id) {
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			if(info == null) {
				return "login";
			}
			
			long member_id = info.getMember_id();
			
			boolean result = siteInquiryService.deleteInquiryFile(inquiries_file_id, member_id);
			if(!result) {
				return "fail";
			}
			
		} catch (Exception e) {
			log.error("deleteInquiryFile : ", e);
			throw e;
		}
		return "success";
	}
		

}	
