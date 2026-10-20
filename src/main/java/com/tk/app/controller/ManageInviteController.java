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
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageInviteService;
import com.tk.app.mapper.ManageInviteMapper;
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
    
    @ModelAttribute("domainKey")
    public String setDomainKey(@PathVariable("domainKey") String domainKey) {
        return domainKey;
    }

    // 워크스페이스 초대는 현재 도메인의 PM만 가능
    private Long requireWorkspacePm(String domainKey) {
        SessionInfo info = LoginMemberUtil.getSessionInfo();
        if (info == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        Map<String, Object> map = new HashMap<>();
        map.put("domainKey", domainKey);
        Long workspaceId = manageInviteService.getWorkspaceIdByDomainKey(map);
        if (workspaceId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스가 없습니다.");
        map.put("workspaceId", workspaceId);
        map.put("memberId", info.getMember_id());
        if (inviteMapper.countActiveWorkspacePm(map) != 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "워크스페이스 PM만 팀원을 초대할 수 있습니다.");
        }
        return workspaceId;
    }

    @GetMapping("/view")
    public String invitationForm(@PathVariable("domainKey") String domainKey) {
        requireWorkspacePm(domainKey);
        return "manage/invitation";
    }

    @GetMapping("/eligible-members")
    @ResponseBody
    public ResponseEntity<?> getEligibleMembers(
            @PathVariable("domainKey") String domainKey,
            @RequestParam(value = "keyword", required = false) String keyword) {
        try {
            Long workspaceId = requireWorkspacePm(domainKey);
            Map<String, Object> map = new HashMap<>();
            map.put("workspaceId", workspaceId);
            map.put("keyword", keyword); 
            
            List<ManageInviteDto> eligibleList = myInviteService.getSearchEligibleMembers(map);
            
            return ResponseEntity.ok(eligibleList);
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.info("getEligibleMembers 오류 : ", e);
            return ResponseEntity.badRequest().body("목록 조회 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<?> sendInvitations(
            @PathVariable("domainKey") String domainKey,
            @RequestBody Map<String, Object> requestData) {
        
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                log.info("로그인 정보가 존재하지 않습니다.");
                return ResponseEntity.status(401).body("로그인 정보가 존재하지 않습니다.");
            }

            Long workspaceId = requireWorkspacePm(domainKey);

            @SuppressWarnings("unchecked")
            List<Integer> targetMemberIds = (List<Integer>) requestData.get("memberIds");
            
            if (targetMemberIds == null || targetMemberIds.isEmpty()) {
                return ResponseEntity.badRequest().body("초대할 회원을 선택해주세요.");
            }

            Map<String, Object> inviteMap = new HashMap<>();
            inviteMap.put("domainKey", domainKey);
            inviteMap.put("workspaceId", workspaceId);
            inviteMap.put("inviterId", info.getMember_id());
            inviteMap.put("targetMemberIds", targetMemberIds);

            myInviteService.sendInvitesAndMail(inviteMap);

            return ResponseEntity.ok("선택한 회원에게 초대 메일이 성공적으로 발송되었습니다.");

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.info("sendInvitations 오류 : ", e);
            return ResponseEntity.badRequest().body("서버 처리 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/accept")
    public String acceptInvitation(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("inviteKey") String inviteKey,
            Model model) throws Exception {
        
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            
            if (info == null) {
                log.info("초대 수락을 위한 로그인 정보가 존재하지 않습니다.");
                return "redirect:/login"; 
            }

            Map<String, Object> acceptMap = new HashMap<>();
            acceptMap.put("inviteKey", inviteKey);
            acceptMap.put("memberId", info.getMember_id());
            acceptMap.put("domainKey", domainKey);

            myInviteService.acceptInvite(acceptMap);

        } catch (IllegalArgumentException e) {
            log.info("acceptInvitation 유효성 에러 : ", e);
            model.addAttribute("message", e.getMessage());
            return "manage/inviteError"; 
            
        } catch (Exception e) {
            log.error("acceptInvitation 시스템 에러 : ", e);
            model.addAttribute("message", "워크스페이스 가입 처리 중 데이터베이스 오류가 발생했습니다. 로그를 확인해 주세요.");
            return "manage/inviteError";
        }
        
        return "redirect:/" + domainKey + "/home/dashboard";
    }
}