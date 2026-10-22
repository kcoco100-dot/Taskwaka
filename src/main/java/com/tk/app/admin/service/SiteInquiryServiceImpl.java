package com.tk.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.mapper.SiteInquiryMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteInquiryServiceImpl implements SiteInquiryService{
	private final SiteInquiryMapper siteInquiryMapper;
	
	@Override
	public int insertInquiry(SiteInquiryDto dto) {
		try {
			return siteInquiryMapper.insertInquiry(dto);
		} catch (Exception e) {
			log.error("insertInquiry : ",e);
			throw e;
		}
	}

	@Override
	public List<SiteInquiryDto> selectMyInquiryList(long member_id) {
		try {
			return siteInquiryMapper.selectMyInquiryList(member_id);
		} catch (Exception e) {
			log.error("selectMyInquiryList : ",e);
			throw e;
		}
	}

}
