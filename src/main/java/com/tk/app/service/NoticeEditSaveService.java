package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.NoticeFileDto;
import com.tk.app.mapper.NoticeMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NoticeEditSaveService {
	private final NoticeMapper noticeMapper;
	
	public NoticeEditSaveService(NoticeMapper noticeMapper) {
		this.noticeMapper = noticeMapper;
	}
	
	// newFiles : NoticeService 에서 업로드를 마친 파일의 정보
	//		    : DB 등록에 필요한 원본 이름, 서버 저장 이름, 경로, 크기 등이 들어있음
	// deleteFiles : NoticeService 에서 현재 공지에 속하는지 검증한 기존 파일 정보
	@Transactional(rollbackFor = Exception.class)
	public int saveNoticeEdit(
			long workspaceId, long noticeId, String title, String content,
			List<NoticeFileDto> newFiles, List<NoticeFileDto> deleteFiles
			) throws Exception {
		
		// 메서드 본문에 제목, 내용 수정 및 결과 검사
		int updatedCount = noticeMapper.noticeEdit(workspaceId, noticeId, title, content);
		
		// 현재 공지 1개를 수정하는 것이기 때문에 반드시 1이어야 함
		if(updatedCount != 1) {
			throw new Exception("공지를 수정할 수 없습니다.");
		}
		
		// 새 파일 등록하기
		if(newFiles != null) {
			for(NoticeFileDto noticeFile : newFiles) {
				// noticeFile :  반복에서 꺼낸 새 파일
				// noticeId : 수정중인 공지 번호
				noticeFile.setNoticeId(noticeId);
				
				int fileInsertCount = noticeMapper.insertNoticeFile(noticeFile);
				
				// 등록 행 수 검사
				if(fileInsertCount != 1) {
					throw new Exception("첨부파일 정보를 등록할 수 없습니다.");
				}
			}
			
		}
		
		// 삭제 대상의 파일 삭제
		if(deleteFiles != null) {
			for(NoticeFileDto deleteFile : deleteFiles) {
				
				int fileDeleteCount = noticeMapper.deleteNoticeFile(workspaceId, noticeId, deleteFile.getNoticeFileId());
			
				if(fileDeleteCount != 1) {
					throw new Exception("첨부파일 정보를 삭제할 수 없습니다.");
				}
			}
		}
		
		// 공지 몇 건을 수정했는지
		return updatedCount;
	}
}
