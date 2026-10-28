package com.tk.app.service;

import com.tk.app.domain.dto.MypageDto;

public interface MypageService {

	public MypageDto findMyInfo(Long memberId) throws Exception;
}
