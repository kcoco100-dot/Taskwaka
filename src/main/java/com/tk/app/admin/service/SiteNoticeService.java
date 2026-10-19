package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.SiteNoticeDto;

public interface SiteNoticeService {
	public List<SiteNoticeDto> selectNoticeList();
	public int insertNotice(SiteNoticeDto dto);
	public SiteNoticeDto selectNoticeDetail(long notice_id);
	public int updateNotice(SiteNoticeDto dto);
}
