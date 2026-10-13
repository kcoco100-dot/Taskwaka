package com.tk.app.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.common.MyUtil;
import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageProjectController {

	@Autowired
	private ManageService servive;
	
	@Autowired
    private MyUtil myUtil;
	
	@ModelAttribute("domainKey")
	public String setDomainKey(
			@PathVariable("domainKey") String domainKey) {
		
		return domainKey;
	}
	
	// 프로젝트 페이지
	@GetMapping("/view")
	public String projectView(@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
			HttpServletRequest req,
			Model model) throws Exception {
		
		try {
			int size = 10;
			int dataCount = 10;
			int totalPage = 0;
			
	        Map<String, Object> map = new HashMap<>();
	        map.put("domainKey", domainKey);
	        
	        dataCount = servive.dataCount(map);
	        totalPage = (dataCount + size -1) /size;
	        
	        if(totalPage == 0) totalPage = 1;
	        current_page = Math.min(current_page, totalPage);
	        
	        int offset = (current_page - 1) * size;
	        if(offset < 0) offset = 0;
	        
	        map.put("offset", offset);
	        map.put("size", size);
	        
	        List<ManageDto> projectList =servive.listProject(map);
	        
	        model.addAttribute("projectList", projectList);
	        model.addAttribute("dataCount", dataCount);
	        model.addAttribute("size", size);
	        model.addAttribute("totalPage", totalPage);
	        model.addAttribute("page", current_page);
	        
		} catch (Exception e) {
			log.info("projectView : ", e);
		}

		return "manage/view";
	}
	
	// 프로젝트 등록 페이지
	@GetMapping("/write")
	public String writeForm(Model model) {
		try {
			model.addAttribute("mode", "write");
			model.addAttribute("dto", new ManageDto());
		} catch (Exception e) {
			log.info("writeForm : ", e);
		}
		
		return "manage/viewWrite";
	}
	
	// 프로젝트 등록 처리
	@PostMapping("/write")
	public String writeSubmit(@PathVariable("domainKey") String domainKey,
			ManageDto dto) throws Exception {
		
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) {
				log.info("로그인 정보가 존재하지 않습니다.");
				return "redirect:/login";
			}
			
			Long workspaceId = servive.findByDomainKey(domainKey);
			if (workspaceId == null) {
				throw new IllegalArgumentException("유효하지 않은 워크스페이스 도메인입니다: " + domainKey);
			}
			
			dto.setWorkspace_id(workspaceId);
			dto.setMember_id(info.getMember_id());
			
			dto.setProgress(0);
			
			servive.insertProject(dto);
			
		} catch (Exception e) {
			log.info("writeSubmit : ", e);
		}
		
		return "redirect:/" + domainKey + "/manage/view";
	}
	
	// 프로젝트 상세조회
	@GetMapping("/detail/{projectId}")
    public String projectDetail(@PathVariable("domainKey") String domainKey, 
    			@PathVariable("projectId") Long projectId, 
    			Model model) {
        try {
	        Map<String, Object> map = new HashMap<>();
	        map.put("projectId", projectId);
            List<ManageDto> projectList = servive.getProjectDetail(map);
            
            if(projectList == null) {
                return "redirect:/" + domainKey + "/manage/view"; 
            }
            
            ManageDto project = projectList.get(0);
            
            model.addAttribute("project", project);
            
        } catch (Exception e) {
            log.error("projectDetail : ", e);
        }
        
        return "manage/projectDetail";
    }
	
	@GetMapping("update")
	public String updateForm(@PathVariable("domainKey") String domainKey,
			@PathVariable(name = "projectId") Long projectId,
			Model model) throws Exception {
		
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			
			
		} catch (Exception e) {
			log.info("updateForm : ", e);
		}
		
		return "redirect:/" + domainKey + "/manage/view";
	}
	
}
