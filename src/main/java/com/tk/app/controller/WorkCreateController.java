package com.tk.app.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.domain.dto.WorkCreateDto;
import com.tk.app.service.WorkCreateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/work")
public class WorkCreateController {
	private final WorkCreateService service;
	
	@GetMapping
	public String createForm() {
		return "work/create";
	}
	
	@PostMapping
	public String insertWork(
			@RequestParam(name = "status") int status,
			Authentication authentication,
			WorkCreateDto dto,
			RedirectAttributes redirectAttributes,
			Model model) {
		
		try {
			dto.setStatus(status);
			
			if(authentication == null || ! authentication.isAuthenticated()) {
				return "redirect:/member/login";
			}
			
			String loginId = authentication.getName();
			
			service.insertWork(dto, loginId);
			
			redirectAttributes.addFlashAttribute("message", "워크가 생성되었습니다.");
			
			return "redirect:/main";
			
        } catch (IllegalArgumentException e) {
            log.error("워크 생성 입력 오류 : {}", e.getMessage());

            String message = e.getMessage();
            String errorField = null;

            if (message != null) {

                if (message.contains("워크명")) {
                    errorField = "workSubject";

                } else if (message.contains("도메인")) {
                    errorField = "domainKey";

                } else if (message.contains("소개")) {
                    errorField = "workContent";

                } else if (message.contains("공개 여부")) {
                    errorField = "status";
                }
            }

            model.addAttribute("errorMessage", message);
            model.addAttribute("errorField", errorField);
            model.addAttribute("workCreateDto", dto);
		} catch (Exception e) {
		    log.error("insertWork : ", e);
		    
		    model.addAttribute("errorMessage", "워크 생성 중 오류가 발생했습니다.");
		    model.addAttribute("workCreateDto", dto);
		}
		
		return "work/create";
	}
	
	@GetMapping("/check")
	@ResponseBody
	public Map<String, Boolean> checkDomainKey(@RequestParam("domainKey") String domainKey){
		
	    if (domainKey.length() > 255 ||
	            !domainKey.matches("[a-z0-9]+")) {
	        throw new IllegalArgumentException(
	            "도메인 주소 형식이 올바르지 않습니다.");
	    }
		
	    boolean duplicate = service.isDomainKeyDuplicate(domainKey);
		
		return Map.of("duplicate" , duplicate);
	}
	
	
	
}
