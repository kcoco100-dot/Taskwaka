package com.tk.app.service;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.MypageDto;
import com.tk.app.mapper.MypageMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MypageServiceImpl implements MypageService {
	
	private final MypageMapper mapper;
	
	
	@Override
	public MypageDto findMyInfo(Long memberId) throws Exception {
		return mapper.findMyInfo(memberId);
	}

}
