package com.tk.app.admin.controller;

import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.admin.domain.dto.AdminMemberDto;
import com.tk.app.admin.service.AdminMemberService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {
	private final AdminMemberService service;

    @GetMapping
    public String adminMain() {
        return "redirect:/admin/user";
    }

    @GetMapping("/user")
    public String user(Model model,
    		@RequestParam (name="keyword", required = false) String keyword,
    		@RequestParam (name="status", required = false) Integer status ) {
    	if(keyword == null) {
    		keyword = "";
    	}
    	
    	keyword = keyword.trim();
    	
    	List<AdminMemberDto> members =
    	        service.searchMembersByStatus(keyword, status);

    	for (AdminMemberDto member : members) {
    	    member.setWorkspaces(
    	        service.findMemberWorkspaces(member.getMember_id())
    	    );
    	}

    	model.addAttribute("members", members);
    	
        return adminPage("user", model);
    }
    
    @GetMapping("/user/{member_id}")
    public String userDetail(@PathVariable long member_id,
    		Model model) {
    	  System.out.println("회원 상세 컨트롤러 실행: " + member_id);
    	
    	model.addAttribute("workspaces", service.findMemberWorkspaces(member_id));
    	model.addAttribute("member", service.findMemberById(member_id));
    	
    	return adminPage("userDetail", model);
    }

    
    @PostMapping("/user/status")
    public String updateMemberStatus(
    		@RequestParam ("member_id") long member_id,
    		@RequestParam ("status") int status) {
    	
    	if(status != 0 && status != 1) {
    		return "redirect:/admin/user";
    	}
    	
    	service.updateMemberStatus(member_id, status);
    	
    	return "redirect:/admin/user";
    }
    
    @GetMapping("/domain")
    public String domain(Model model) {
        return adminPage("domain", model);
    }

    @GetMapping("/notice")
    public String notice(Model model) {
        return adminPage("notice", model);
    }

    @GetMapping("/inquiry")
    public String inquiry(Model model) {
        return adminPage("inquiry", model);
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        return adminPage("stats", model);
    }

    @GetMapping("/security")
    public String security(Model model) {
        return adminPage("security", model);
    }

    private String adminPage(String section, Model model) {

        model.addAttribute("activeSection", section);
        model.addAttribute("adminName", "관리자 (ADMIN)");

        return "admin/admin_layout";
    }
}