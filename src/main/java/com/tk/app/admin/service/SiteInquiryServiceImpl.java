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

	@Override
	public SiteInquiryDto selectInquiryDetail(long inquiry_id, long member_id) {
		try {
			return siteInquiryMapper.selectInquiryDetail(inquiry_id, member_id);
		} catch (Exception e) {
			log.error("selectInquiryDetail : ",e);
			throw e;
		}
	}

	@Override
	public List<SiteInquiryDto> selectAllInquiryList() {
		try {
			return siteInquiryMapper.selectAllInquiryList();
		} catch (Exception e) {
			log.error("selectAllInquiryList : ",e);
			throw e;
		}
	}

	@Override
	public SiteInquiryDto selectAdminInquiryDetail(long inquiry_id) {
		try {
			return siteInquiryMapper.selectAdminInquiryDetail(inquiry_id);
		} catch (Exception e) {
			log.error("selectAdminInquiryDetail : ",e);
			throw e;
		}
	}

	@Override
	public int updateInquiryAnswer(SiteInquiryDto dto) {
		try {
			return siteInquiryMapper.updateInquiryAnswer(dto);
		} catch (Exception e) {
			log.error("updateInquiryAnswer : ",e);
			throw e;
		}
	}

	@Override
	public int updateInquiry(SiteInquiryDto dto) {
		try {
			return siteInquiryMapper.updateInquiry(dto);
		} catch (Exception e) {
			log.error("updateInquiry : ",e);
			throw e;
		}
	}

	@Override
	public boolean canEditInquiry(long inquiry_id, long member_id) {
		try {
			SiteInquiryDto inquiry = siteInquiryMapper.selectInquiryDetail(inquiry_id, member_id);
			
			if(inquiry == null) {
			    return false;
			}

			return inquiry.getStatus() == 0;
			
			
		} catch (Exception e) {
			log.error("canEditInquiry : ",e);
			throw e;
		}
	}

}
