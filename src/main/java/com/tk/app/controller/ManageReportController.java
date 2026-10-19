package com.tk.app.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageReportService;
import com.tk.app.service.ManageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/{domainKey}/manage/approval")
public class ManageReportController {

	private final ManageReportService service;
	private final ManageService manageService;

//	@ModelAttribute("domainKey")
//	public String setDomainKey(@PathVariable("domainKey") String domainKey) {
//		return domainKey;
//	}

	// 보고서 목록 및 검색
	 @GetMapping({"", "/"})
	    public String listReport(
	            @PathVariable("domainKey") String domainKey,
	            @RequestParam(value = "page", defaultValue = "1") int page,
	            @RequestParam(value = "reportCategory", defaultValue = "0") int reportCategory,
	            @RequestParam(value = "reviewStatus", defaultValue = "-2") int reviewStatus,
	            @RequestParam(value = "keyword", defaultValue = "") String keyword,
	            Model model) {

	        int size = 10;
	        int dataCount = 0;
	        int totalPage = 0;

	        page = Math.max(1, page);

	        List<ManageReportDto> list = Collections.emptyList();
	        String errorMessage = null;

	        try {
	            if (reportCategory < 0 || reportCategory > 2 || reviewStatus < -2 || reviewStatus > 2) {
	                throw new IllegalArgumentException("검색 조건이 올바르지 않습니다.");
	            }

	            Map<String, Object> map = requestMap(domainKey);

	            map.put("reportCategory", reportCategory);
	            map.put("reviewStatus", reviewStatus);
	            map.put("keyword", keyword.trim());

	            int count = service.countReport(map);
	            int pages = (int) Math.ceil((double) count / size);

	            if (pages > 0 && page > pages) {
	                page = pages;
	            }

	            map.put("offset", (page - 1) * size);
	            map.put("size", size);

	            List<ManageReportDto> result = service.listReport(map);

	            dataCount = count;
	            totalPage = pages;
	            list = result;

	        } catch (Exception e) {
	            log.error("보고서 목록 조회 오류: ", e);
	            errorMessage = "보고서를 조회하는 중 오류가 발생했습니다.";
	        }

	        model.addAttribute("domainKey", domainKey);
	        model.addAttribute("reportCategory", reportCategory);
	        model.addAttribute("reviewStatus", reviewStatus);
	        model.addAttribute("keyword", keyword);

	        model.addAttribute("list", list);
	        model.addAttribute("dataCount", dataCount);
	        model.addAttribute("page", page);
	        model.addAttribute("size", size);
	        model.addAttribute("totalPage", totalPage);
	        model.addAttribute("errorMessage", errorMessage);

	        return "manage/approval";
	    }

	// 보고서 상세 조회 및 최초 열람
    @GetMapping("/detail")
    public String reportDetail(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("reportCategory") int reportCategory,
            @RequestParam("reportId") Long reportId,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            if (reportCategory != 1 && reportCategory != 2) {
                throw new IllegalArgumentException("보고서 유형이 올바르지 않습니다.");
            }
            Map<String, Object> map = requestMap(domainKey);
            map.put("reportCategory", reportCategory);
            map.put("reportId", reportId);

            ManageReportDto dto = service.openReport(map);
            model.addAttribute("domainKey", domainKey);
            model.addAttribute("dto", dto);
            return "manage/reportDetail";

        } catch (Exception e) {
            log.error("보고서 상세 조회 오류: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/" + domainKey + "/manage/approval";
        }
    }

    // 보고서 승인 및 반려
    @PostMapping("/updateReview")
    public String updateReview(
			@PathVariable("domainKey") String domainKey,
	        @RequestParam("reportCategory") int reportCategory,
	        @RequestParam("reportId") Long reportId,
	        @RequestParam("reviewStatus") int reviewStatus,
	        @RequestParam(value = "reviewComment", required = false) String reviewComment,
	        @RequestParam(value = "rejectReason", required = false) String rejectReason,
	        RedirectAttributes redirectAttributes) {
	
	    try {
	        if (reportCategory != 1 && reportCategory != 2) {
	            throw new IllegalArgumentException("보고서 유형이 올바르지 않습니다.");
	        }
	        Map<String, Object> map = requestMap(domainKey);
	        map.put("reportCategory", reportCategory);
	        map.put("reportId", reportId);
	        map.put("reviewStatus", reviewStatus);
	        map.put("reviewComment", reviewComment == null ? null : reviewComment.trim());
	        map.put("rejectReason", rejectReason == null ? null : rejectReason.trim());
	
	        service.updateReview(map);
	        redirectAttributes.addFlashAttribute("message", reviewStatus == 1 ? "승인했습니다." : "반려했습니다.");
	
	    } catch (Exception e) {
	        log.error("보고서 결재 처리 오류: ", e);
	        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
	    }
	
	    return "redirect:/" + domainKey + "/manage/approval/detail"
	            + "?reportCategory=" + reportCategory + "&reportId=" + reportId;
	}

    // 로그인 회원과 워크스페이스 조회 조건 생성
    private Map<String, Object> requestMap(String domainKey) throws Exception {
        SessionInfo info = LoginMemberUtil.getSessionInfo();
        if (info == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        Long workspaceId = manageService.findByDomainKey(domainKey);
        if (workspaceId == null) {
            throw new IllegalStateException("워크스페이스를 찾을 수 없습니다.");
        }

        Map<String, Object> map = new HashMap<>();
        map.put("workspaceId", workspaceId);
        map.put("memberId", info.getMember_id());
        return map;
    }
}