package com.tk.app.siteNoticeTicker;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member/api/site-notice-ticker")
public class SiteNoticeTickerController {
    private final SiteNoticeTickerService siteNoticeTickerService;

    @GetMapping
    public ResponseEntity<List<SiteNoticeTickerDto>> newNotices() {
        SessionInfo member = LoginMemberUtil.getSessionInfo();
        if (member == null || member.getMember_id() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        return ResponseEntity.ok(siteNoticeTickerService.findNewNotices());
    }
}
