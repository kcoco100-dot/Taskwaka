package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.ManageNoticeDto;
import com.tk.app.service.ManageNoticeService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageNoticeController {
	private final ManageNoticeService service;
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;
	
	// 공지사항 조회
	@GetMapping("/noticeList")
	public String noticeList(@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
			@RequestParam(name = "schType", defaultValue = "all") String schType,
			@RequestParam(name = "kwd", defaultValue = "") String kwd,
			HttpServletRequest req,
			Model model) {
		
		try {
			int size = 10;
			int dataCount = 0;
			int totalPage = 0;
			
			kwd = myUtil.decodeUrl(kwd);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("schType", schType);
			map.put("kwd", kwd);
			map.put("domainKey", domainKey);
			
			dataCount = service.dataCount(map);
			if (dataCount != 0) {
				totalPage = paginateUtil.pageCount(dataCount, size);
			}
			
			current_page = Math.min(current_page, totalPage);
			
			
			int offset = (current_page - 1) * size;
			if(offset < 0) offset = 0;
			
			map.put("offset", offset);
			map.put("size", size);
			
			List<ManageNoticeDto> list = service.selectNoticeList(map);
			
			String cp = RequestUtils.getContextPath();
			String query = "";
			String listUrl = cp + "/" + domainKey + "/manage/noticeList";
			if (!kwd.isBlank()) {
				query = "schType=" + schType + "&kwd=" + myUtil.encodeUrl(kwd);
				listUrl += "?" + query;
			}

			String paging = paginateUtil.paging(current_page, totalPage, listUrl);
			
			model.addAttribute("domainKey", domainKey);
			model.addAttribute("list", list);
			model.addAttribute("page", current_page);
			model.addAttribute("dataCount", dataCount);
			model.addAttribute("size", size);
			model.addAttribute("totalPage", totalPage);
			model.addAttribute("paging", paging);

			model.addAttribute("schType", schType);
			model.addAttribute("kwd", kwd);
			
			
		} catch (Exception e) {
			log.info("noticeList : ", e);
		}
		
		
		return "manage/noticeList";
	}
	
	
}
