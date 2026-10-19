package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.SiteNoticeDto;

public interface SiteNoticeService {
	public List<SiteNoticeDto> selectNoticeList();
	public List<SiteNoticeDto> searchNotice(String keyword);
	public int insertNotice(SiteNoticeDto dto);
	public SiteNoticeDto selectNoticeDetail(long notice_id);
	public int updateNotice(SiteNoticeDto dto);
	public int deleteNotice(long notice_id);
	public int dataCountNotice(String keyword);
	public List<SiteNoticeDto> selectNoticePage(String keyword, int offset, int size);
}
