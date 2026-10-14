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
            
            int offset = (page - 1) * size;
            
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("workspaceId", workspaceId);
            paramMap.put("kwd", kwd);
            paramMap.put("offset", offset);
            paramMap.put("size", size);

            List<ManageMeetingsDto> list = service.findMeetingList(paramMap);
            int totalCount = service.countMeetings(paramMap);
            int totalPage = (int) Math.ceil((double) totalCount / size);

            response.put("meetingList", list);
            
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("totalCount", totalCount);
            pageInfo.put("totalPage", totalPage);
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
            Long workspaceId = manageService.findByDomainKey(domainKey);
            
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("workspaceId", workspaceId);
            paramMap.put("meetingMinuteId", meetingMinuteId);
            
            List<ManageMeetingsDto> resultList = service.findMeetingsById(paramMap);
            
            if (resultList == null || resultList.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(resultList.get(0));
            
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
            Long workspaceId = manageService.findByDomainKey(domainKey);
            
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("workspaceId", workspaceId);
            paramMap.put("meetingMinuteId", meetingMinuteId);
            paramMap.put("title", dto.getTitle());
            paramMap.put("content", dto.getContent());
            
            service.updateMeetings(paramMap);
            
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
            Long workspaceId = manageService.findByDomainKey(domainKey);
            
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("workspaceId", workspaceId);
            paramMap.put("meetingMinuteId", meetingMinuteId);
            
            service.deleteMeetings(paramMap);
            
            resultMap.put("status", "success");
            return ResponseEntity.ok(resultMap);
        } catch (Exception e) {
            log.error("회의록 삭제 에러: ", e);
            resultMap.put("status", "fail");
            return ResponseEntity.internalServerError().body(resultMap);
        }
    }	
}