package com.tk.app.admin.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.domain.dto.SiteInquiryFileDto;

public interface SiteInquiryService {
	
	// 클라이언트 페이지
	public int insertInquiry(SiteInquiryDto dto, MultipartFile[] files);
	public List<SiteInquiryDto> selectMyInquiryList(long member_id);
	public SiteInquiryDto selectInquiryDetail(long inquiry_id, long member_id);
	public int updateInquiry(SiteInquiryDto dto, MultipartFile[] files);
	public int deleteInquiry(SiteInquiryDto dto);
	// 수정 가능한 상태인지
	public boolean canEditInquiry(long inquiry_id, long member_id);
	
	public List<SiteInquiryFileDto> selectInquiryFileList(long inquiry_id);
	public SiteInquiryFileDto selectInquiryFile(long inquiries_file_id);
	public boolean deleteInquiryFile(long inquiries_file_id, long member_id);
	

	
	// 관리자 페이지
	public List<SiteInquiryDto> selectAllInquiryList();
	public SiteInquiryDto selectAdminInquiryDetail(long inquiry_id);
	public int updateInquiryAnswer(SiteInquiryDto dto);
	public int deleteAdminInquiry(SiteInquiryDto dto);

}
