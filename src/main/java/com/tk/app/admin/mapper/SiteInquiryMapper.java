package com.tk.app.admin.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.SiteInquiryDto;

@Mapper
public interface SiteInquiryMapper {
	// 클라이언트 페이지
	public int insertInquiry(SiteInquiryDto dto);
	public List<SiteInquiryDto> selectMyInquiryList(long member_id);
	public SiteInquiryDto selectInquiryDetail(@Param("inquiry_id") long inquiry_id, @Param("member_id") long member_id);
	public int updateInquiry(SiteInquiryDto dto);
	
	// 관리자 페이지
	public List<SiteInquiryDto> selectAllInquiryList();
	public SiteInquiryDto selectAdminInquiryDetail(long inquiry_id);
	public int updateInquiryAnswer(SiteInquiryDto dto);
	
}
