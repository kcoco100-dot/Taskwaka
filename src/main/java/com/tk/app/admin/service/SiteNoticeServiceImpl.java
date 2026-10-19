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
	public List<SiteNoticeDto> searchNotice(String keyword) {
		try {
			return siteNoticeMapper.searchNotice(keyword);
		} catch (Exception e) {
			log.error("searchNotice 오류 : ", e);
			
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

	@Override
	public int deleteNotice(long notice_id) {
		try {
			return siteNoticeMapper.deleteNotice(notice_id);
		} catch (Exception e) {
			log.error("deleteNotice 오류 : ", e);
			
			throw e;
		}
	}

	@Override
	public int dataCountNotice(String keyword) {
		try {
			return siteNoticeMapper.dataCountNotice(keyword);
		} catch (Exception e) {
			log.error("dataCountNotice 오류 : ", e);
			
			throw e;
		}
	}

	@Override
	public List<SiteNoticeDto> selectNoticePage(String keyword, int offset, int size) {
		try {
			return siteNoticeMapper.selectNoticePage(keyword, offset, size);
		} catch (Exception e) {
			log.error("selectNoticePage 오류 : ", e);
			
			throw e;
		}
	}


	
}
