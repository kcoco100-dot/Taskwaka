package com.tk.app.siteNoticeTicker;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteNoticeTickerMapper {
    public List<SiteNoticeTickerDto> findNewNotices();
}
