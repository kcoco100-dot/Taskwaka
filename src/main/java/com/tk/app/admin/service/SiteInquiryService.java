package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.SiteInquiryDto;

public interface SiteInquiryService {
	public int insertInquiry(SiteInquiryDto dto);
	public List<SiteInquiryDto> selectMyInquiryList(long member_id);

}
