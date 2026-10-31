package com.tk.app.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.tk.app.domain.dto.MypageDto;
import com.tk.app.domain.dto.MypagePwdDto;
import com.tk.app.domain.dto.MypageWorkDto;

public interface MypageService {

	public MypageDto findMyInfo(Long memberId) throws Exception;
	public boolean isSnsMember(Long memberId) throws Exception;
	public int updateContact(MypageDto dto) throws Exception;
	
	public String updateProfile(Long memberId, MultipartFile file) throws Exception;
	public void deleteProfile(Long memberId) throws Exception;
	
	public String updatePwd(MypagePwdDto dto) throws Exception;
	
	public List<MypageWorkDto> findMyWorkspaces(Long memberId) throws Exception;
	
	public boolean hasOngoingWork(Long memberId, Long workId) throws Exception;
	
	public void leaveWork(Long memberId, Long workId);

    public boolean hasOtherWorkMembers(Long memberId, Long workId);
    public void deleteOwnedWork(Long memberId, Long workId);

    public boolean hasActiveWorkspaces(Long memberId);
    public void withdrawSite(Long memberId);
	
}
