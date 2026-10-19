package com.tk.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.SiteNoticeDto;
import com.tk.app.admin.mapper.SiteNoticeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteNoticeServiceImpl implements SiteNoticeService {
	private final SiteNoticeMapper siteNoticeMapper;
	
	@Override
	public List<SiteNoticeDto> selectNoticeList() {
		try {
			return siteNoticeMapper.selectNoticeList();
		} catch (Exception e) {
			log.error("selectNoticeList 오류 : ", e);
			
			throw e;
		}
		
	}

	@Override
	public int insertNotice(SiteNoticeDto dto) {
		try {
			return siteNoticeMapper.insertNotice(dto);
		} catch (Exception e) {
			log.error("insertNotice 오류 : ", e);
			
			throw e;
		}
	}

	@Override
	public SiteNoticeDto selectNoticeDetail(long notice_id) {
		try {
			return siteNoticeMapper.selectNoticeDetail(notice_id);
		} catch (Exception e) {
			log.error("selectNoticeDetail 오류 : ", e);
			
			throw e;
		}
	}

	@Override
	public int updateNotice(SiteNoticeDto dto) {
		try {
			return siteNoticeMapper.updateNotice(dto);
		} catch (Exception e) {
			log.error("updateNotice 오류 : ", e);
			
			throw e;
		}
		
	}
	
}
