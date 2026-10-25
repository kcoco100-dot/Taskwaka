package com.tk.app.admin.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.domain.dto.SiteInquiryFileDto;

@Mapper
public interface SiteInquiryMapper {
	// 클라이언트 페이지
	public int insertInquiry(SiteInquiryDto dto);
	public int insertInquiryFile(SiteInquiryFileDto dto);
	public List<SiteInquiryDto> selectMyInquiryList(long member_id);
	public SiteInquiryDto selectInquiryDetail(@Param("inquiry_id") long inquiry_id, @Param("member_id") long member_id);
	public int updateInquiry(SiteInquiryDto dto);
	public int deleteInquiry(SiteInquiryDto dto);
	
	public List<SiteInquiryFileDto> selectInquiryFileList(long inquiry_id);
	public SiteInquiryFileDto selectInquiryFile(long inquiries_file_id);
	public int deleteInquiryFile(long inquiries_file_id);
	public int deleteInquiryFiles(long inquiry_id);
	
	// 관리자 페이지
	public List<SiteInquiryDto> selectAllInquiryList();
	public SiteInquiryDto selectAdminInquiryDetail(long inquiry_id);
	public int updateInquiryAnswer(SiteInquiryDto dto);
	public int deleteAdminInquiry(SiteInquiryDto dto);

	
}
