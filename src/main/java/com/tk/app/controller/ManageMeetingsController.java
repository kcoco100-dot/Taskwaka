package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.domain.dto.ManageMeetingsDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageMeetingsService;
import com.tk.app.service.ManageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage/api/meetings") // 경로를 API 용으로 명확히 지정
public class ManageMeetingsController {

	private final ManageMeetingsService service;

	private final ManageService manageService;
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;

	// 회의록 조회
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getMeetingList(
			@PathVariable("domainKey") String domainKey,
			@RequestParam(value = "kwd", defaultValue = "") String kwd,
			@RequestParam(value = "page", defaultValue = "1") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		Map<String, Object> response = new HashMap<>();

		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) {
				return ResponseEntity.status(401).build(); // 401 Unauthorized
			}

			Long workspaceId = manageService.findByDomainKey(domainKey);
			if (workspaceId == null) {
				return ResponseEntity.badRequest().build();
			}

			if (size < 1 || size > 100) size = 10;
			kwd = myUtil.decodeUrl(kwd == null ? "" : kwd).trim();
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("workspaceId", workspaceId);
			paramMap.put("memberId", info.getMember_id());
			paramMap.put("kwd", kwd);
			int totalCount = 0;
			int totalPage = 0;
			totalCount = service.countMeetings(paramMap);
			if (totalCount != 0) {
				totalPage = paginateUtil.pageCount(totalCount, size);
			}
			page = Math.min(page, totalPage);
			if (page < 1) page = 1;
			int offset = (page - 1) * size;
			paramMap.put("offset", offset);
			paramMap.put("size", size);
			List<ManageMeetingsDto> list = service.findMeetingList(paramMap);

			response.put("meetingList", list);

			Map<String, Object> pageInfo = new HashMap<>();
			pageInfo.put("totalCount", totalCount);
			pageInfo.put("totalPage", totalPage);
			pageInfo.put("page", page);
			response.put("pageInfo", pageInfo);

			response.put("currentMemberId", info.getMember_id());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("회의록 목록 조회 에러: ", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	// 회의록 상세 조회
	@GetMapping("/{meetingMinuteId}")
	public ResponseEntity<ManageMeetingsDto> getMeetingDetail(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("meetingMinuteId") Long meetingMinuteId) {

		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) return ResponseEntity.status(401).build();
			Long workspaceId = manageService.findByDomainKey(domainKey);

			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("workspaceId", workspaceId);
			paramMap.put("meetingMinuteId", meetingMinuteId);
			paramMap.put("memberId", info.getMember_id());

			List<ManageMeetingsDto> resultList = service.findMeetingsById(paramMap);

			if (resultList == null || resultList.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			ManageMeetingsDto dto = resultList.get(0);
			dto.setCanDelete(true);
			return ResponseEntity.ok(dto);

		} catch (Exception e) {
			log.error("회의록 상세 조회 에러: ", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	// 회의록 수정
	@PutMapping("/{meetingMinuteId}")
	public ResponseEntity<Map<String, Object>> updateMeeting(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("meetingMinuteId") Long meetingMinuteId,
			@RequestBody ManageMeetingsDto dto) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) return ResponseEntity.status(401).build();
			Long workspaceId = manageService.findByDomainKey(domainKey);

			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("workspaceId", workspaceId);
			paramMap.put("meetingMinuteId", meetingMinuteId);
			paramMap.put("memberId", info.getMember_id());
			paramMap.put("title", dto.getTitle());
			paramMap.put("content", dto.getContent());

			if (service.updateMeetings(paramMap) != 1) return ResponseEntity.status(403).build();

			resultMap.put("status", "success");
			return ResponseEntity.ok(resultMap);
		} catch (Exception e) {
			log.error("회의록 수정 에러: ", e);
			resultMap.put("status", "fail");
			return ResponseEntity.internalServerError().body(resultMap);
		}
	}

	// 회의록 삭제
	@DeleteMapping("/{meetingMinuteId}")
	public ResponseEntity<Map<String, Object>> deleteMeeting(
			@PathVariable("domainKey") String domainKey,
			@PathVariable("meetingMinuteId") Long meetingMinuteId) {

		Map<String, Object> resultMap = new HashMap<>();
		try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if (info == null) return ResponseEntity.status(401).build();
			Long workspaceId = manageService.findByDomainKey(domainKey);

			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("workspaceId", workspaceId);
			paramMap.put("meetingMinuteId", meetingMinuteId);
			paramMap.put("memberId", info.getMember_id());

			if (service.deleteMeetings(paramMap) != 1) return ResponseEntity.status(403).build();

			resultMap.put("status", "success");
			return ResponseEntity.ok(resultMap);
		} catch (Exception e) {
			log.error("회의록 삭제 에러: ", e);
			resultMap.put("status", "fail");
			return ResponseEntity.internalServerError().body(resultMap);
		}
	}
}
