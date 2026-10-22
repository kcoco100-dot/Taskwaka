package com.tk.app.admin.mapper;


import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.admin.domain.dto.SiteInquiryDto;

@Mapper
public interface SiteInquiryMapper {
	public int insertInquiry(SiteInquiryDto dto);
	public List<SiteInquiryDto> selectMyInquiryList(long member_id);
}
