package com.tk.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.SiteNoticeDto;
import com.tk.app.admin.domain.dto.SiteNoticeFileDto;
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
			log.error("selectNoticeList : ", e);
			
			throw e;
		}
		
	}
	
	@Override
	public List<SiteNoticeDto> searchNotice(String keyword) {
		try {
			return siteNoticeMapper.searchNotice(keyword);
		} catch (Exception e) {
			log.error("searchNotice : ", e);
			
			throw e;
		}
		
	}

	@Override
	public int insertNotice(SiteNoticeDto dto) {
		try {
			return siteNoticeMapper.insertNotice(dto);
		} catch (Exception e) {
			log.error("insertNotice : ", e);
			
			throw e;
		}
	}

	@Override
	public SiteNoticeDto selectNoticeDetail(long notice_id) {
		try {
			return siteNoticeMapper.selectNoticeDetail(notice_id);
		} catch (Exception e) {
			log.error("selectNoticeDetail : ", e);
			
			throw e;
		}
	}

	@Override
	public int updateNotice(SiteNoticeDto dto) {
		try {
			return siteNoticeMapper.updateNotice(dto);
		} catch (Exception e) {
			log.error("updateNotice : ", e);
			
			throw e;
		}
		
	}

	@Override
	public int deleteNotice(long notice_id) {
		try {
			return siteNoticeMapper.deleteNotice(notice_id);
		} catch (Exception e) {
			log.error("deleteNotice : ", e);
			
			throw e;
		}
	}

	@Override
	public int dataCountNotice(String keyword) {
		try {
			return siteNoticeMapper.dataCountNotice(keyword);
		} catch (Exception e) {
			log.error("dataCountNotice : ", e);
			
			throw e;
		}
	}

	@Override
	public List<SiteNoticeDto> selectNoticePage(String keyword, int offset, int size) {
		try {
			return siteNoticeMapper.selectNoticePage(keyword, offset, size);
		} catch (Exception e) {
			log.error("selectNoticePage : ", e);
			
			throw e;
		}
	}

	@Override
	public int insertNoticeFile(SiteNoticeFileDto dto) {
		try {
			return siteNoticeMapper.insertNoticeFile(dto);
		} catch (Exception e) {
			log.error("insertNoticeFile : ", e);
			
			throw e;
		}
		
	}

	@Override
	public List<SiteNoticeFileDto> selectNoticeFiles(long notice_id) {
		try {
			return siteNoticeMapper.selectNoticeFiles(notice_id);
		} catch (Exception e) {
			log.error("siteNoticeMapper : ", e);
			
			throw e;
		}
	}

	@Override
	public SiteNoticeFileDto selectNoticeFile(long notice_file_id) {
		try {
			return siteNoticeMapper.selectNoticeFile(notice_file_id);
		} catch (Exception e) {
			log.error("selectNoticeFile : ", e);
			throw e;
		}
	}

	@Override
	public int deleteNoticeFile(long notice_file_id) {
		try {
			return siteNoticeMapper.deleteNoticeFile(notice_file_id);
		} catch (Exception e) {
			log.error("deleteNoticeFile : ", e);
			throw e;
		}
	}

	@Override
	public int dataCountPublicNotice(String keyword) {
		try {
		    return siteNoticeMapper.dataCountPublicNotice(keyword);

		} catch (Exception e) {
			log.error("dataCountPublicNotice : ", e);
			throw e;
		}
	}

	@Override
	public List<SiteNoticeDto> selectPublicNoticePage(String keyword, int offset, int size) {
		try {
		    return siteNoticeMapper.selectPublicNoticePage(keyword, offset, size);

		} catch (Exception e) {
			log.error("selectPublicNoticePage : ", e);
			throw e;
		}
	}


	
}
