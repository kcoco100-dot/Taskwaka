package com.tk.app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.MypageDto;

@Mapper
public interface MypageMapper {
	
	public MypageDto findMyInfo(Long memberId);
	public int countSnsAccount(Long memberId);
	
    public int countActivePhone(MypageDto dto);
    public int updateContact(MypageDto dto);
    
    public int updateProfile(MypageDto dto);
	
}
