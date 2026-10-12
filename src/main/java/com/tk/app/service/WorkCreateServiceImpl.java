package com.tk.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.WorkCreateDto;
import com.tk.app.mapper.WorkCreateMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkCreateServiceImpl implements WorkCreateService {
	private final WorkCreateMapper mapper;
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void insertWork(WorkCreateDto dto, Long LoginMemberId) throws Exception {
		
		if(dto == null || LoginMemberId == null) {
			throw new IllegalArgumentException ("잘못된 요청입니다.");
		}
		
		if(dto.getWorkSubject() == null || dto.getWorkSubject().isBlank()) {
			throw new IllegalArgumentException ("워크명을 입력해주세요.");
		}
		
		
	}

}
