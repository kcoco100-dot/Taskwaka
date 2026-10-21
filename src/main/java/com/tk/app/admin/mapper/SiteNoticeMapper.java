package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.SiteNoticeDto;
import com.tk.app.admin.domain.dto.SiteNoticeFileDto;

@Mapper
public interface SiteNoticeMapper {
	// 전체 조회
	public List<SiteNoticeDto> selectNoticeList();
	// 검색
	public List<SiteNoticeDto> searchNotice(@Param("keyword") String keyword);
	// 작성
	public int insertNotice(SiteNoticeDto dto);
	// 상세보기
	public SiteNoticeDto selectNoticeDetail(long notice_id);
	// 수정
	public int updateNotice(SiteNoticeDto dto);
	// 글지우기
	public int deleteNotice(long notice_id);
	// 페이지 전체 개수
	public int dataCountNotice(@Param("keyword") String keyword);
	// 페이징 개수
	public List<SiteNoticeDto> selectNoticePage(
			@Param("keyword") String keyword, 
			@Param("offset") int offset, 
			@Param("size") int size);
	// 첨부 파일 정보 저장
	public int insertNoticeFile(SiteNoticeFileDto dto);
	// 여러 파일 조회
	public List<SiteNoticeFileDto> selectNoticeFiles(long notice_id);
	// 파일 하나 조회
	public SiteNoticeFileDto selectNoticeFile(long notice_file_id);
	// 파일 삭
	public int deleteNoticeFile(long notice_file_id);
	
	
}
