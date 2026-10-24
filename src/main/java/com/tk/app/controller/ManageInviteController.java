package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tk.app.domain.dto.ManageInviteDto;
import com.tk.app.mapper.ManageInviteMapper;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageInviteService;

import com.tk.app.service.ManageWorkspaceInviteService; 

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage/invite")
public class ManageInviteController {

    private final ManageInviteService manageInviteService;
    private final ManageWorkspaceInviteService myInviteService;
    private final ManageInviteMapper inviteMapper;

    // 도메인키 설정
    @ModelAttribute("domainKey")
    public String setDomainKey(@PathVariable("domainKey") String domainKey) {
        return domainKey;
    }

    // 워크스페이스 조회
    private Long requireWorkspaceContext(String domainKey) {
        SessionInfo info = LoginMemberUtil.getSessionInfo();
        if (info == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("domainKey", domainKey);
        Long workspaceId = manageInviteService.getWorkspaceIdByDomainKey(map);
        if (workspaceId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스가 없습니다.");
        }
        return workspaceId;
    }

    // 팀원 초대 화면
    @GetMapping("/view")
    public String invitationForm(@PathVariable("domainKey") String domainKey) {
        requireWorkspaceContext(domainKey);
        return "manage/invitation";
    }

    // 초대 대상 조회
    @GetMapping("/eligible-members")
    @ResponseBody
    public ResponseEntity<?> getEligibleMembers(
            @PathVariable("domainKey") String domainKey,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd) {
        try {
            Long workspaceId = requireWorkspaceContext(domainKey);
            Map<String, Object> map = new HashMap<>();
            map.put("workspaceId", workspaceId);
            map.put("schType", schType);
            map.put("kwd", kwd); 
            
            List<ManageInviteDto> eligibleList = myInviteService.getSearchEligibleMembers(map);
            
            return ResponseEntity.ok(eligibleList);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.info("getEligibleMembers 오류 : ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // 팀원 초대 전송
    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<?> sendInvitations(
            @PathVariable("domainKey") String domainKey,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                log.info("로그인 정보가 존재하지 않습니다.");
                return ResponseEntity.status(401).build();
            }

            Long workspaceId = requireWorkspaceContext(domainKey);

            @SuppressWarnings("unchecked")
            List<Integer> targetMemberIds = (List<Integer>) requestData.get("memberIds");
            
            if (targetMemberIds == null || targetMemberIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            Map<String, Object> inviteMap = new HashMap<>();
            inviteMap.put("domainKey", domainKey);
            inviteMap.put("workspaceId", workspaceId);
            inviteMap.put("inviterId", info.getMember_id());
            inviteMap.put("targetMemberIds", targetMemberIds);

            myInviteService.sendInvitesAndMail(inviteMap);

            return ResponseEntity.ok().build();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.info("sendInvitations 오류 : ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // 초대 수락
    @GetMapping("/accept")
    public String acceptInvitation(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("inviteKey") String inviteKey,
            Model model) throws Exception {
        
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            
            if (info == null) {
                log.info("초대 수락을 위한 로그인 정보가 존재하지 않습니다.");
                throw new IllegalStateException("로그인 정보가 없습니다."); 
            }

            Map<String, Object> acceptMap = new HashMap<>();
            acceptMap.put("inviteKey", inviteKey);
            acceptMap.put("memberId", info.getMember_id());
            acceptMap.put("domainKey", domainKey);

            myInviteService.acceptInvite(acceptMap);

        } catch (IllegalArgumentException e) {
            log.info("acceptInvitation 유효성 에러 : ", e);
            throw e; 
            
        } catch (Exception e) {
            log.error("acceptInvitation 시스템 에러 : ", e);
            throw e;
        }
        
        return "redirect:/" + domainKey + "/home/dashboard";
    }
}
