package com.tk.app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.MypageDto;

@Mapper
public interface MypageMapper {

    MypageDto findMyInfo(Long memberId);
}
