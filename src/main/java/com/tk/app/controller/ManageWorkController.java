package com.tk.app.controller;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.domain.dto.ManageWorkDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageService;
import com.tk.app.service.ManageWorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageWorkController {
    private final ManageService manageService;
    private final ManageWorkService service;
    private final PaginateUtil paginateUtil;
    private final MyUtil myUtil;
    
    // 도메인키 설정
    @ModelAttribute("domainKey")
    public String setDomainKey(@PathVariable("domainKey") String domainKey) {
        return domainKey;
    }
    
    // 워크 팀원 목록 조회
    @GetMapping("/workspaceRoles")
    public String listWorkMember(@PathVariable("domainKey") String domainKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd, Model model) throws Exception {
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            
            Long workspaceId = manageService.findByDomainKey(domainKey);
            kwd = kwd == null ? "" : kwd.trim();
            
            int size = 10;
            int dataCount = service.dataCount(workspaceId, info.getMember_id(), kwd);
            int totalPage = paginateUtil.pageCount(dataCount, size);
            
            page = Math.max(1, Math.min(page, totalPage));
            List<ManageWorkDto> memberList = service.listWorkMember(workspaceId, info.getMember_id(), kwd, page, size);
            String listUrl = RequestUtils.getContextPath() + "/" + domainKey + "/manage/workspaceRoles?schType=" + myUtil.encodeUrl(schType) + "&kwd=" + myUtil.encodeUrl(kwd);
            
            boolean pmChangePending = service.hasPendingChangeRequest(workspaceId);
            
            model.addAttribute("memberList", memberList);
            model.addAttribute("pmChangePending", pmChangePending);
            model.addAttribute("pmChangePending", service.hasPendingChangeRequest(workspaceId));
            model.addAttribute("pmChangeHistory", service.listChangeRequests(workspaceId, info.getMember_id()));
            
            model.addAttribute("schType", schType);
            model.addAttribute("kwd", kwd);
            model.addAttribute("page", page);
            model.addAttribute("dataCount", dataCount);
            model.addAttribute("totalPage", totalPage);
            model.addAttribute("paging", paginateUtil.paging(page, totalPage, listUrl));
            model.addAttribute("pageBaseUrl", listUrl + "&page=");
            
        } catch (Exception e) {
            log.info("listWorkMember : ", e);
            
            throw e;
        }
        
        return "manage/workspaceRoles";
    }
    
    // 워크 직위 변경
    @PostMapping("/workspaceRoles/update")
    public String updateWorkRole(@PathVariable("domainKey") String domainKey,
            @RequestParam("workspaceMemberId") Long workspaceMemberId,
            @RequestParam("role") int role,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd, RedirectAttributes ra) throws Exception {
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            Long workspaceId = manageService.findByDomainKey(domainKey);
            service.updateWorkRole(workspaceId, info.getMember_id(), workspaceMemberId, role);
            ra.addAttribute("page", page);
            ra.addAttribute("schType", schType);
            ra.addAttribute("kwd", kwd);
        } catch (Exception e) {
            log.info("updateWorkRole : ", e);
            throw e;
        }
        return "redirect:/" + domainKey + "/manage/workspaceRoles";
    }
    
    // 사장 교체 신청
    @PostMapping("/workspaceRoles/requestChange")
    public String manageChage(@PathVariable("domainKey") String domainKey,
    		@RequestParam("newMemberId") Long newMemberId,
    		@RequestParam("reason") String reason,
    		@RequestParam(value = "page", defaultValue = "1") int page,
    		RedirectAttributes ra) throws Exception {
    	try {
			SessionInfo info = LoginMemberUtil.getSessionInfo();
			if(info == null) {
				throw new IllegalAccessException("로그인 정보가 없습니다.");
			}
			
			Long workspaceId = manageService.findByDomainKey(domainKey);
			service.requestChange(workspaceId, info.getMember_id(), newMemberId, reason);
			
			ra.addAttribute("page", page);
			
		} catch (Exception e) {
			log.info("manageChage : ", e);
			
			throw e;
		}
    	
    	return "redirect:/" + domainKey + "/manage/workspaceRoles";
    }
    		
    		
    
}
