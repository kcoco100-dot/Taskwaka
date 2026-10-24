package com.tk.app.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseBody;
import com.tk.app.domain.dto.ManageFileDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.common.StorageService;
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
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;
	
	private final StorageService storageService;

	// 조회 조건 생성
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
        map.put("domainKey", domainKey);
		map.put("memberId", info.getMember_id());
		return map;
	}

	// 보고서 목록 조회
	@GetMapping({"", "/"})
	public String listReport(@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "page", defaultValue = "1") int current_page,
			@RequestParam(value = "reportCategory", defaultValue = "0") int reportCategory,
			@RequestParam(value = "reviewStatus", defaultValue = "0") int reviewStatus,
			@RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
			Model model) throws Exception {
		int size = 10;
		int dataCount = 0;
		int totalPage = 0;
		List<ManageReportDto> list = Collections.emptyList();
		Map<String, Object> statusCounts = Collections.emptyMap();
		String paging = "";
		String pageBaseUrl = "";
		try {
			if (reportCategory < 0 || reportCategory > 2 || reviewStatus < 0 || reviewStatus > 4) {
				throw new IllegalArgumentException("검색 조건이 올바르지 않습니다.");
			}
			kwd = myUtil.decodeUrl(kwd == null ? "" : kwd).trim();
			Map<String, Object> map = requestMap(domainKey);
			map.put("reportCategory", reportCategory);
			map.put("reviewStatus", reviewStatus);
			map.put("schType", schType);
            map.put("kwd", kwd);
			dataCount = service.countReport(map);
			if (dataCount > 0) {
				totalPage = paginateUtil.pageCount(dataCount, size);
			}
			current_page = Math.min(current_page, totalPage);
			if (current_page < 1) {
				current_page = 1;
			}
			int offset = (current_page - 1) * size;
			if (offset < 0) {
				offset = 0;
			}
			map.put("offset", offset);
			map.put("size", size);
			list = service.listReport(map);
			String listUrl = RequestUtils.getContextPath() + "/" + domainKey + "/manage/approval"
					+ "?reportCategory=" + reportCategory + "&reviewStatus=" + reviewStatus
					+ "&schType=" + myUtil.encodeUrl(schType) + "&kwd=" + myUtil.encodeUrl(kwd);
			paging = paginateUtil.paging(current_page, totalPage, listUrl);
			pageBaseUrl = listUrl + "&page=";
			try {
				Map<String, Object> statusMap = new HashMap<>(map);
				statusMap.put("reportCategory", 0);
				statusMap.put("reviewStatus", 0);
				statusMap.put("kwd", "");
				Map<String, Object> counts = service.countReportStatuses(statusMap);
				if (counts != null) {
					statusCounts = counts;
				}
			} catch (Exception e) {
				log.info("countReportStatuses : ", e);
			}
		} catch (Exception e) {
			log.info("listReport : ", e);
            throw e;
		}
		model.addAttribute("domainKey", domainKey);
		model.addAttribute("reportCategory", reportCategory);
		model.addAttribute("reviewStatus", reviewStatus);
		model.addAttribute("schType", schType);
            model.addAttribute("kwd", kwd);
		model.addAttribute("list", list);
		model.addAttribute("dataCount", dataCount);
		model.addAttribute("page", current_page);
		model.addAttribute("size", size);
		model.addAttribute("totalPage", totalPage);
		model.addAttribute("paging", paging);
		model.addAttribute("pageBaseUrl", pageBaseUrl);
		model.addAttribute("uncheckedCount", statusCounts.getOrDefault("uncheckedCount", 0));
		model.addAttribute("pendingCount", statusCounts.getOrDefault("pendingCount", 0));
		model.addAttribute("approvedCount", statusCounts.getOrDefault("approvedCount", 0));
		model.addAttribute("rejectedCount", statusCounts.getOrDefault("rejectedCount", 0));
		return "manage/approval";
	}

	// 보고서 상세 조회
	@GetMapping("/detail")
	public String reportDetail(
			@PathVariable("domainKey") String domainKey,
			@RequestParam("reportCategory") int reportCategory,
			@RequestParam("reportId") Long reportId,
            @RequestParam(value = "listCategory", defaultValue = "0") int listCategory,
            @RequestParam(value = "reviewStatus", defaultValue = "0") int reviewStatus,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            @RequestParam(value = "page", defaultValue = "1") int page,
			Model model) throws Exception {

		try {
			if (reportCategory != 1 && reportCategory != 2) {
                throw new IllegalArgumentException("보고서 유형이 올바르지 않습니다.");
            }
            if (listCategory < 0 || listCategory > 2 || reviewStatus < 0 || reviewStatus > 4) {
                throw new IllegalArgumentException("검색 조건이 올바르지 않습니다.");
            }
            Map<String, Object> map = requestMap(domainKey);
			map.put("reportCategory", reportCategory);
			map.put("reportId", reportId);

			ManageReportDto dto = service.openReport(map);
            model.addAttribute("files", service.listReportFiles(map));
            map.put("currentCategory", reportCategory);
            map.put("reportCategory", listCategory);
            map.put("reviewStatus", reviewStatus);
            map.put("schType", schType);
            map.put("kwd", kwd.trim());
            model.addAttribute("prevReport", service.selectPrevReport(map));
            model.addAttribute("nextReport", service.selectNextReport(map));
            model.addAttribute("listCategory", listCategory);
            model.addAttribute("reviewStatus", reviewStatus);
            model.addAttribute("schType", schType);
            model.addAttribute("kwd", kwd);
            model.addAttribute("page", page);
			model.addAttribute("domainKey", domainKey);
			model.addAttribute("dto", dto);

		} catch (Exception e) {
			log.info("reportDetail : ", e);
            throw e;
		}
        return "manage/reportDetail";
	}

	// 보고서 승인 및 반려
	@PostMapping("/updateReview")
	public String updateReview(
			@PathVariable("domainKey") String domainKey,
	        @RequestParam("reportCategory") int reportCategory,
	        @RequestParam("reportId") Long reportId,
	        @RequestParam("reviewStatus") int reviewStatus,
	        @RequestParam(value = "reviewComment", required = false) String reviewComment,
	        @RequestParam(value = "rejectReason", required = false) String rejectReason) throws Exception {

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

	    } catch (Exception e) {
	        log.info("updateReview : ", e);
            throw e;
	    }

	    return "redirect:/" + domainKey + "/manage/approval/detail"
	            + "?reportCategory=" + reportCategory + "&reportId=" + reportId;
	}

    // 보고서 등록 화면
    @GetMapping({"/write", "/write/{projectId}"})
    public String writeForm(@PathVariable("domainKey") String domainKey,
            @PathVariable(name = "projectId", required = false) Long projectId, Model model) throws Exception {
        try {
            Map<String, Object> map = requestMap(domainKey);
            ManageReportDto dto = new ManageReportDto();
            dto.setProjectId(projectId);
            dto.setProgress(0.0);
            model.addAttribute("dto", dto);
            model.addAttribute("domainKey", domainKey);
            model.addAttribute("projectOptions", service.listWritableProjects(map));
            model.addAttribute("files", Collections.emptyList());
            model.addAttribute("mode", "write");
        } catch (Exception e) {
            log.info("writeForm : ", e);
            throw e;
        }
        return "manage/reportwrite";
    }
    // 보고서 등록
    @PostMapping("/write")
    public String writeSubmit(@PathVariable("domainKey") String domainKey, ManageReportDto dto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws Exception {
        try {
            service.createReport(dto, requestMap(domainKey), files);
        } catch (Exception e) {
            log.info("writeSubmit : ", e);
            throw e;
        }
        return "redirect:/" + domainKey + "/manage/approval";
    }
    // 보고서 수정 화면
    @GetMapping("/update/{reportId}")
    public String updateForm(@PathVariable("domainKey") String domainKey,
            @PathVariable("reportId") Long reportId, Model model) throws Exception {
        try {
            Map<String, Object> map = requestMap(domainKey);
            map.put("reportCategory", 1);
            map.put("reportId", reportId);
            model.addAttribute("dto", service.getReport(map));
            model.addAttribute("domainKey", domainKey);
            model.addAttribute("projectOptions", service.listWritableProjects(map));
            model.addAttribute("files", service.listReportFiles(map));
            model.addAttribute("mode", "update");
        } catch (Exception e) {
            log.info("updateForm : ", e);
            throw e;
        }
        return "manage/reportwrite";
    }
    // 보고서 수정
    @PostMapping("/update/{reportId}")
    public String updateSubmit(@PathVariable("domainKey") String domainKey,
            @PathVariable("reportId") Long reportId, ManageReportDto dto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "deleteFileIds", required = false) List<Long> deleteFileIds) throws Exception {
        try {
            Map<String, Object> map = requestMap(domainKey);
            map.put("reportId", reportId);
            service.editReport(dto, map, files, deleteFileIds);
        } catch (Exception e) {
            log.info("updateSubmit : ", e);
            throw e;
        }
        return "redirect:/" + domainKey + "/manage/approval/detail?reportCategory=1&reportId=" + reportId;
    }
    // 보고서 삭제
    @PostMapping("/delete/{reportId}")
    public String deleteSubmit(@PathVariable("domainKey") String domainKey,
            @PathVariable("reportId") Long reportId) throws Exception {
        try {
            Map<String, Object> map = requestMap(domainKey);
            map.put("reportId", reportId);
            service.deleteReport(map);
        } catch (Exception e) {
            log.info("deleteSubmit : ", e);
            throw e;
        }
        return "redirect:/" + domainKey + "/manage/approval";
    }
    // 보고서 첨부파일 다운로드
    @GetMapping("/files/{fileId}")
    @ResponseBody
    public ResponseEntity<?> downloadFile(@PathVariable("domainKey") String domainKey,
            @PathVariable("fileId") Long fileId, @RequestParam("reportCategory") int reportCategory,
            @RequestParam("reportId") Long reportId) throws Exception {
        ResponseEntity<?> response = ResponseEntity.notFound().build();
        try {
            Map<String, Object> map = requestMap(domainKey);
            map.put("reportCategory", reportCategory);
            map.put("reportId", reportId);
            for (ManageFileDto file : service.listReportFiles(map)) {
                if (fileId.equals(file.getFileId())) {
                    response = storageService.downloadFile(file.getPath(), file.getStoredName(), file.getOriginalName());
                    break;
                }
            }
        } catch (Exception e) {
            log.info("downloadFile : ", e);
            throw e;
        }
        return response;
    }
}
