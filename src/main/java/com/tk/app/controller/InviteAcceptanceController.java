package com.tk.app.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.net.URI;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.tk.app.mapper.ManageInviteMapper;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageWorkspaceInviteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class InviteAcceptanceController {
    private static final String KEY = "invite.preview.key";
    private static final String NONCE = "invite.preview.confirmation";
    private final ManageWorkspaceInviteService invites;
    private final ManageInviteMapper mapper;

    public static String previewPath(String token) {
        try {
            String canonical = UUID.fromString(token).toString();
            if (canonical.equalsIgnoreCase(token)) return "/member/invite?inviteKey=" + canonical;
        } catch (IllegalArgumentException | NullPointerException ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 초대 주소입니다.");
    }

    public static String afterLogin(HttpServletRequest request, HttpServletResponse response, String normalTarget) {
        var cache = new HttpSessionRequestCache();
        var saved = cache.getRequest(request, response);
        if (saved == null) return normalTarget;
        String uri = URI.create(saved.getRedirectUrl()).getPath();
        String context = request.getContextPath();
        String[] keys = saved.getParameterValues("inviteKey");
        cache.removeRequest(request, response);
        if ("GET".equals(saved.getMethod()) && keys != null && keys.length == 1 &&
                (uri.equals(context + "/main") || uri.equals(context + "/member/invite") ||
                 uri.startsWith(context + "/") && uri.substring(context.length()).matches("/[a-zA-Z0-9-]+/manage/invite/accept"))) {
            return previewPath(keys[0]);
        }
        return normalTarget;
    }

    @GetMapping("/member/invite")
    public String preview(@RequestParam("inviteKey") String token, HttpSession session, Model model) {
        previewPath(token);
        var member = LoginMemberUtil.getSessionInfo();
        if (member == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (mapper.previewInvite(token, member.getMember_id()) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "수락 가능한 초대를 찾지 못했습니다.");
        String nonce = UUID.randomUUID().toString();
        session.setAttribute(KEY, token);
        session.setAttribute(NONCE, nonce);
        model.addAttribute("confirmation", nonce);
        return "member/invitePreview";
    }

    @PostMapping("/member/invite")
    public String accept(@RequestParam("confirmation") String confirmation, HttpSession session) throws Exception {
        var member = LoginMemberUtil.getSessionInfo();
        if (member == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String token = (String) session.getAttribute(KEY);
        if (token == null || !Objects.equals(confirmation, session.getAttribute(NONCE)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Map<String,Object> map = new HashMap<>();
        map.put("inviteKey", token);
        map.put("memberId", member.getMember_id());
        try {
            invites.acceptInvite(map);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        session.removeAttribute(KEY);
        session.removeAttribute(NONCE);
        return "redirect:/" + map.get("domainKey") + "/home/dashboard";
    }
}
