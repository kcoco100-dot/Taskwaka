package com.tk.app.siteNoticeTicker;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteNoticeTickerService {
    private final SiteNoticeTickerMapper siteNoticeTickerMapper;

    public List<SiteNoticeTickerDto> findNewNotices() {
        return siteNoticeTickerMapper.findNewNotices();
    }
}
