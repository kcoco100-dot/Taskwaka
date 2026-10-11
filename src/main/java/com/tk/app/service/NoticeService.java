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
	
	// 워크 공지 목록 조회
	public List<NoticeDto> findWorkspaceNotice(long workspaceId) {
		
		return noticeMapper.findWorkspaceNotice(workspaceId);
	}
	
	// 워크에 공개된 공지 상세조회
	public NoticeDto findWorkspaceDetailNotice(long workspaceId, long noticeId) {
		
		return noticeMapper.findWorkspaceDetailNotice(workspaceId, noticeId);
	}
}
