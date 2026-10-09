package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.mapper.NoticeMapper;

@Service
public class NoticeService {
	
	private final NoticeMapper noticeMapper;
	
	public NoticeService(NoticeMapper noticeMapper) {
		this.noticeMapper = noticeMapper;
	}
	
	public List<NoticeDto> findWorkspaceNotice(long workspaceId) {
		
		return noticeMapper.findWorkspaceNotice(workspaceId);
	}
}
