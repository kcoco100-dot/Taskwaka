package com.tk.app.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.domain.dto.NoticeFileDto;
import com.tk.app.mapper.NoticeMapper;

import lombok.extern.slf4j.Slf4j;



@Service
@Slf4j
public class NoticeService {
	
	private final NoticeMapper noticeMapper;
	private final StorageService storageService;
	
	@Value("${file.upload-root}")
	private String uploadPath;
	
	// 공지사항 글 등록 시 파일처리
	public String validateNoticeFiles(List<MultipartFile> files) {
		// MultipartFile : 업로드한 파일 한개
		if(files == null) {
			return null;			
		}
		
		int fileCount = 0;
		
		for(MultipartFile file : files) {
			if(file != null && !file.isEmpty()) {
				fileCount++;
				
				String originalName = file.getOriginalFilename();
				
				if(originalName == null || originalName.isBlank()) {
					return "첨부파일 이름을 확인해 주세요.";
				}
				
				// 원본 파일명 255 바이트 제한 검사
				if(originalName.getBytes(StandardCharsets.UTF_8).length > 255) {
					return "첨부 파일명이 너무 깁니다. 파일명을 줄여 주세요.";
				}
				
				// 파일 명에서 마지막 점의 위치 찾기
				int dotIndex = originalName.lastIndexOf('.');
				
				// 점부터 비교하는데 확장자가 없을 경우
				if(dotIndex <= 0 || dotIndex == originalName.length() - 1) {
					return "확장자가 있는 파일만 첨부 가능합니다.";
				}
				
				// extension : 확장자 종류를 담을 변수
				String extension = originalName.substring(dotIndex);
				
				// 허용할 확장자들 
				// equalsIgnoreCase : 대소문자 구분없이 허용
				if(!extension.equalsIgnoreCase(".gif")
					&& !extension.equalsIgnoreCase(".png")
					&& !extension.equalsIgnoreCase(".txt")
					&& !extension.equalsIgnoreCase(".pdf")
					&& !extension.equalsIgnoreCase(".doc")
					&& !extension.equalsIgnoreCase(".docx")
					&& !extension.equalsIgnoreCase(".ppt")
					&& !extension.equalsIgnoreCase(".pptx")
					&& !extension.equalsIgnoreCase(".xls")
					&& !extension.equalsIgnoreCase(".xlsx")) {
						
					return "허용되지 않은 첨부파일 형식입니다.";
				}
			}
		}
		
		// 파일 첨부 갯수는 5개 까지만 가능
		if(fileCount > 5) {
			return "첨부파일은 5개까지 등록할 수 있습니다.";
		}
		
		return null;
		
	}
	
	public NoticeService(NoticeMapper noticeMapper, StorageService storageService) {
		this.noticeMapper = noticeMapper;
		this.storageService = storageService;
	}
	
	// 워크 공지 목록 조회
	public List<NoticeDto> findWorkspaceNotice(long workspaceId) {
		
		return noticeMapper.findWorkspaceNotice(workspaceId);
	}
	
	// 워크에 공개된 공지 상세조회
	public NoticeDto findWorkspaceDetailNotice(long workspaceId, long noticeId) {
		
		return noticeMapper.findWorkspaceDetailNotice(workspaceId, noticeId);
	}
	
	// 공지 상세조회에서 첨부된 파일 조회
	public List<NoticeFileDto> findWorkspaceFiles(
			long workspaceId, long noticeId) {
		
		return noticeMapper.findWorkspaceFiles(workspaceId, noticeId);
	}
	
	// 공지사항 등록 및 파일처리
	@Transactional(rollbackFor = Exception.class)
	public int createWorkspaceNotice(
			long workspaceId, long memberId, String title, String content,
			List<MultipartFile> files) throws Exception {
		
		// storageService 가 반환한 서버 저장 파일명을 넣을 함수
		List<String> storedNames = new ArrayList<>();
		
		try {
			
			// 파일처리를 위한 변수들
			NoticeDto notice = new NoticeDto();
			notice.setWorkspaceId(workspaceId);
			notice.setMemberId(memberId);
			notice.setTitle(title);
			notice.setContent(content);
			
			// insertCount : 공지 한 행이 저장됐는지 확인할 변수
			int insertCount = noticeMapper.createWorkspaceNotice(notice);
			
			if(insertCount != 1 || notice.getNoticeId() == null) {
				throw new Exception("공지를 등록할 수 없습니다.");
			}
			
			// 미 선택 , 빈 파일은 건너뜀
			if(files != null) {
				for(MultipartFile file : files) {
					if(file == null || file.isEmpty()) {
						continue;
					}
					
					String storedName = storageService.uploadFileToServer(file, uploadPath);
					
					if(storedName == null || storedName.isBlank()) {
						throw new Exception("첨부파일을 저장할 수 없습니다.");
					}
					
					storedNames.add(storedName);
					
					NoticeFileDto noticeFile = new NoticeFileDto();
					noticeFile.setNoticeId(notice.getNoticeId());
					noticeFile.setOriginalName(file.getOriginalFilename());
					noticeFile.setStoredName(storedName);
					noticeFile.setFilePath(uploadPath);
					noticeFile.setFileSize(file.getSize());
					
					int fileInsertCount = noticeMapper.insertNoticeFile(noticeFile);
					
					if(fileInsertCount != 1) {
						throw new Exception("첨부파일 정보를 등록할 수 없습니다.");
					}
				}
			}
			
			return insertCount;
			
		} catch (Exception e) {
			for(String storedName : storedNames) {
				try {
					boolean deleted = storageService.deleteFile(uploadPath, storedName);
					
					if(!deleted) {
						log.error("첨부파일 정리 실패:" + storedName);
					}
				} catch (Exception e2) {
					log.error("첨부파일 정리 중 오류가 발생했습니다.", e2);
					
				}
			}
			
			throw e;
		}
	}
	
	// 공지사항 수정
	public int noticeEdit(long workspaceId, long noticeId, String title, String content) {
		
		return noticeMapper.noticeEdit(workspaceId, noticeId, title, content);
	}
	
	// 공지사항 삭제
	public int deleteNotice(long workspaceId, long noticeId) {
		
		return noticeMapper.deleteNotice(workspaceId, noticeId);
	}
}
