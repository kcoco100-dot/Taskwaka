package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.MyTaskDto;
import com.tk.app.mapper.MyTaskMapper;

@Service
public class MyTaskService {
	
	private final MyTaskMapper myTaskMapper;
	
	public MyTaskService(MyTaskMapper myTaskMapper) {
		this.myTaskMapper = myTaskMapper;
	}
	
	// 내 업무 목록 조회
	public List<MyTaskDto> findTasksList(long workspaceId, long memberId) {
		
		return myTaskMapper.findTasksList(workspaceId, memberId);
	}
}
