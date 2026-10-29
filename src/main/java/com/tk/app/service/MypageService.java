package com.tk.app.service;

import org.springframework.web.multipart.MultipartFile;

import com.tk.app.domain.dto.MypageDto;

public interface MypageService {

	public MypageDto findMyInfo(Long memberId) throws Exception;
	public boolean isSnsMember(Long memberId) throws Exception;
	public int updateContact(MypageDto dto) throws Exception;
	
	public String updateProfile(Long memberId, MultipartFile file) throws Exception;
	public void deleteProfile(Long memberId) throws Exception;
	
}
