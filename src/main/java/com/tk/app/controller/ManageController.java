package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.service.ManageService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageController {

	@Autowired
	private ManageService servive;
	
	public String setDomainKey(
			@PathVariable("domainKey") String domainKey) {
		
		return domainKey;
	}
	
	// 대시보드 페이지
	@GetMapping
	public String dashBoard() {
		return "manage/pmDashboard";
	}
	
	// 팀원관리 페이지
	@GetMapping("manage")
	public String manage() {
		return "manage/manage";
	}
	
	// 프로젝트 페이지
	@GetMapping("/view")
	public String projectView(Model model) throws Exception {
		
		try {
			// 검색 및 페이징 처리를 위해 사용될 Map 객체 (현재는 빈 상태로 전달)
	        Map<String, Object> map = new HashMap<>();
	        
	        // DB에서 프로젝트 목록을 조회
	        List<ManageDto> projectList =servive.listProject(map);
	        
	        // 조회된 리스트를 "projectList"라는 이름으로 Model에 담아 뷰로 전달
	        model.addAttribute("projectList", projectList);
		} catch (Exception e) {
			// TODO: handle exception
		}

		
		return "manage/view";
	}
	
	// 프로젝트 페이지
	@GetMapping("/article")
	public String projectarticle() {
		return "manage/viewarticle";
	}
	
	// 캘린더 페이지
	@GetMapping("/calender")
	public String calender() {
		return "manage/manageCalender";
	}
	
	
}
