package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.service.ManageReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/{domainKey}/manage/report")
@RequiredArgsConstructor
public class ManageReportController {

	private final ManageReportService service;

	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {
		return domainKey;
	}

	@GetMapping("/list")
	public String listReport(
			@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
			@RequestParam(value = "reportCategory", required = false) Integer reportCategory,
			@RequestParam(value = "reviewStatus", required = false) Integer reviewStatus,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "projectId", required = false) Long projectId,
			@RequestParam(value = "workspaceId", required = false) Long workspaceId,
			HttpServletRequest req,
			Model model) throws Exception {

		try {
			int size = 10;
			int dataCount = 0;
			int totalPage = 0;

			Map<String, Object> map = new HashMap<>();
			map.put("domainKey", domainKey);
			map.put("reportCategory", reportCategory);
			map.put("reviewStatus", reviewStatus);
			map.put("keyword", keyword);
			map.put("projectId", projectId);
			map.put("workspaceId", workspaceId);

			dataCount = service.countReport(map);
			totalPage = (dataCount + size - 1) / size;

			if (totalPage == 0) totalPage = 1;
			current_page = Math.min(current_page, totalPage);

			int offset = (current_page - 1) * size;
			if (offset < 0) offset = 0;

			int start = offset + 1;
			int end = current_page * size;

			map.put("offset", offset);
			map.put("size", size);
			map.put("start", start);
			map.put("end", end);

			List<ManageReportDto> list = service.listReport(map);

			model.addAttribute("list", list);
			model.addAttribute("dataCount", dataCount);
			model.addAttribute("size", size);
			model.addAttribute("totalPage", totalPage);
			model.addAttribute("page", current_page);

			model.addAttribute("reportCategory", reportCategory);
			model.addAttribute("reviewStatus", reviewStatus);
			model.addAttribute("keyword", keyword);
			model.addAttribute("projectId", projectId);

		} catch (Exception e) {
			log.info("listReport : ", e);
		}

		return "manage/approval";
	}

	// 보고서 상세 조회
	@GetMapping("/detail")
	public String reportDetail(
			@PathVariable("domainKey") String domainKey,
			@RequestParam("reviewId") Long reviewId,
			@RequestParam("projectId") Long projectId,
			HttpServletRequest req,
			Model model) throws Exception {

		try {
			Map<String, Object> map = new HashMap<>();
			map.put("domainKey", domainKey);
			map.put("reviewId", reviewId);
			map.put("projectId", projectId);

			List<ManageReportDto> list = service.reportDetail(map);
			ManageReportDto dto = null;
			
			if (list != null && !list.isEmpty()) {
				dto = list.get(0);
			}

			model.addAttribute("dto", dto);

		} catch (Exception e) {
			log.info("reportDetail : ", e);
		}

		return "manage/#";
	}

	// 결재 승인 / 반려 처리
	@PostMapping("/updateReview")
	public String updateReview(
			@PathVariable("domainKey") String domainKey,
			@RequestParam("reviewId") Long reviewId,
			@RequestParam("projectId") Long projectId,
			@RequestParam("reviewStatus") Integer reviewStatus,
			@RequestParam(value = "reviewComment", required = false) String reviewComment,
			@RequestParam(value = "rejectReason", required = false) String rejectReason,
			HttpServletRequest req) throws Exception {

		try {
			Map<String, Object> map = new HashMap<>();
			map.put("domainKey", domainKey);
			map.put("reviewId", reviewId);
			map.put("projectId", projectId);
			map.put("reviewStatus", reviewStatus);
			map.put("reviewComment", reviewComment);
			map.put("rejectReason", rejectReason);

			service.updateReview(map);

		} catch (Exception e) {
			log.info("updateReview : ", e);
		}

		return "redirect:/" + domainKey + "/manage/approval";
	}
}