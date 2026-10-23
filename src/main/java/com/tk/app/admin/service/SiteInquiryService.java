package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.SiteInquiryDto;

public interface SiteInquiryService {
	
	// 클라이언트 페이지
	public int insertInquiry(SiteInquiryDto dto);
	public List<SiteInquiryDto> selectMyInquiryList(long member_id);
	public SiteInquiryDto selectInquiryDetail(long inquiry_id, long member_id);
	public int updateInquiry(SiteInquiryDto dto);
	// 수정 가능한 상태인지
	public boolean canEditInquiry(long inquiry_id, long member_id);

	
	// 관리자 페이지
	public List<SiteInquiryDto> selectAllInquiryList();
	public SiteInquiryDto selectAdminInquiryDetail(long inquiry_id);
	public int updateInquiryAnswer(SiteInquiryDto dto);
}
