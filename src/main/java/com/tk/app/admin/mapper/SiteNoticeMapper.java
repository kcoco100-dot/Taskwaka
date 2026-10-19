package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.admin.domain.dto.SiteNoticeDto;

@Mapper
public interface SiteNoticeMapper {
	public List<SiteNoticeDto> selectNoticeList();
	public int insertNotice(SiteNoticeDto dto);
	public SiteNoticeDto selectNoticeDetail(long notice_id);
	public int updateNotice(SiteNoticeDto dto);
}
