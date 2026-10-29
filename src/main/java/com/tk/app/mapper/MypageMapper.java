package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.MypageDto;
import com.tk.app.domain.dto.MypagePwdDto;
import com.tk.app.domain.dto.MypageWorkDto;

@Mapper
public interface MypageMapper {
	
	public MypageDto findMyInfo(Long memberId);
	public int countSnsAccount(Long memberId);
	
    public int countActivePhone(MypageDto dto);
    public int updateContact(MypageDto dto);
    
    public int updateProfile(MypageDto dto);
    
    public String findPwdByMemberId(Long memberId);
    public int updatePwd(MypagePwdDto dto);
    
    public List<MypageWorkDto> findMyWorkspaces(Long memberId);
  
	
}
