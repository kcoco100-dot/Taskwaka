package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.domain.dto.NoticeFileDto;

@Mapper
public interface NoticeMapper {
	
	// 공지 등록
	public int createWorkspaceNotice(NoticeDto notice);
	
	// 공지 등록 시 파일 업로드 조회
	public List<NoticeFileDto> findWorkspaceFiles(
			@Param("workspaceId") long workspaceId,
			@Param("noticeId") long noticeId);
	
	// 공지 파일처리
	public int insertNoticeFile(NoticeFileDto noticeFile);
	
	// 공지 수정
	// 변경되는 값 : title, content
	public int noticeEdit(
			@Param("workspaceId") long workspaceId,
			@Param("noticeId") long noticeId,
			@Param("title") String title, 
			@Param("content") String content);
	
	// 공지 삭제
	public int deleteNotice(
			@Param("workspaceId") long workspaceId,
			@Param("noticeId") long noticeId);

	// 공개되있는 공지 목록 조회
	public List<NoticeDto> findWorkspaceNotice(
			@Param("workspaceId") long workspaceId);
	
	// 워크에 공개된 공지 상세조회
	public NoticeDto findWorkspaceDetailNotice(
			@Param("workspaceId") long workspaceId,
			@Param("noticeId") long noticeId);

}
