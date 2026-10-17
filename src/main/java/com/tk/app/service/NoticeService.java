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
import com.tk.app.exception.StorageException;
import com.tk.app.exception.StorageFileNotFoundException;
import com.tk.app.mapper.NoticeMapper;

import lombok.extern.slf4j.Slf4j;



@Service
@Slf4j
public class NoticeService {
	
	private final NoticeMapper noticeMapper;
	private final StorageService storageService;
	private final NoticeEditSaveService noticeEditSaveService;
	
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
	
	// 생성자 주입
	public NoticeService(NoticeMapper noticeMapper, StorageService storageService, NoticeEditSaveService noticeEditSaveService) {
		this.noticeMapper = noticeMapper;
		this.storageService = storageService;
		this.noticeEditSaveService = noticeEditSaveService;
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
						throw new StorageException("첨부파일을 저장할 수 없습니다.");
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
	// 수정 시 삭제할 파일이 여러개일 수 있기 때문에 List 로
	// deleteFileIds : 사용자가 삭제하려고 선택한 파일 목록
	// files : 새로 첨부한 파일 목록
	public int noticeEdit(
			long workspaceId, long noticeId, String title, String content,
			List<Long> deleteFileIds, List<MultipartFile> files) throws Exception {
		
		NoticeDto notice = noticeMapper.findWorkspaceDetailNotice(workspaceId, noticeId);
		
		if(notice == null) {
			throw new Exception("수정할 공지가 없습니다.");
		}
		
		// 현재 워크 공지에 속한 기존 파일 전체
		List<NoticeFileDto> noticeFiles = noticeMapper.findWorkspaceFiles(workspaceId, noticeId);
		// 번호 검사를 통과한 실제 삭제 대상 파일 목록
		List<NoticeFileDto> deleteFiles = new ArrayList<>();
		
		if(deleteFileIds != null) {
			for(Long deleteFileId : deleteFileIds) {
				if(deleteFileId == null || deleteFileId <= 0) {
					throw new Exception("삭제할 첨부파일 번호가 올바르지 않습니다.");
				}
				
				// 삭제 요청에 같은 파일 번호가 두번 들어온 경우를 막기 위함
				for(NoticeFileDto deleteFile : deleteFiles) {
					if(deleteFileId.equals(deleteFile.getNoticeFileId())) {
						throw new Exception("삭제할 첨부파일 번호가 중복되었습니다.");
					}
				}
				
				// found : 선택한 번호가 기존 파일 목록에 있었는지 확인하는 변수
				boolean found = false;
				
				for(NoticeFileDto noticeFile : noticeFiles) {
					// 삭제하려고 하는 파일이 기존 공지 파일 목록에 있는지 비교함
					if(deleteFileId.equals(noticeFile.getNoticeFileId())) {
						// 일치하는 파일을 dto 대상에 넣음
						deleteFiles.add(noticeFile);
						// 찾았다 라고 기록한다.
						found = true;
						break;
					}
				}
				
				if(!found) {
					throw new Exception("현재 공지에 속하지 않는 첨부파일입니다.");
				}
			}
		} 
		
		// 새 파일 검증 및 갯수 검사
		String errorMessage = validateNoticeFiles(files);
		// 정상 파일이 아닐때 에러 메시지
		if(errorMessage != null) {
			throw new Exception(errorMessage);
		}
		
		// 새 파일 갯수를 계산할 함수
		int newFileCount = 0;
		
		if(files != null) {
			// 새로 업로드한 files 순회
			for(MultipartFile file : files) {
				// 파일이 존재하고, 비어있지 않을 때만 갯수를 증가시킴
				if(file != null && !file.isEmpty()) {
					newFileCount++;
				}
			}
		}
		
		// 기존 5개중 2개를 삭제하고 새 파일 2개를 추가하면 5-2+2=5 이므로 통과
		int totalFileCount = noticeFiles.size() - deleteFiles.size() + newFileCount;
		if(totalFileCount > 5) {
			throw new Exception("첨부파일은 5개까지만 등록할 수 있습니다.");
		}
		
		// 새 파일 처리를 위한 목록 두개 준비
		List<NoticeFileDto> newFiles = new ArrayList<>();
		List<String> storedNames = new ArrayList<>();
		
		// DB 저장 후 실제 파일을 삭제할 수 있게 반환 위치 옮기기
		int updatedCount;
		
		try {
			// 새 파일이 있으면
			if(files != null) {
				for(MultipartFile file : files) {
					if(file == null || file.isEmpty()) {
						continue;
					}
					// 파일 업로드 및 서버 저장 파일명을 받는 부분
					String storedName = storageService.uploadFileToServer(file, uploadPath);
					
					if(storedName == null || storedName.isBlank()) {
						throw new StorageException("첨부파일을 저장할 수 없습니다.");

					}
					// storedNames 목록에 업로드한 서버 파일명 추가
					storedNames.add(storedName);
					
					// 업로드한 파일 정보를 dto 에 담기
					NoticeFileDto noticeFile = new NoticeFileDto();
					noticeFile.setOriginalName(file.getOriginalFilename());
					noticeFile.setStoredName(storedName);
					noticeFile.setFilePath(uploadPath);
					noticeFile.setFileSize(file.getSize());
					
					newFiles.add(noticeFile);
				}

			}
			
			// 저장 서비스가 반환한 수정 행 수를 updatedCount 에 담음
			updatedCount = noticeEditSaveService.saveNoticeEdit(workspaceId, noticeId, title, content, newFiles, deleteFiles);
			
		} catch (Exception e) {
			// storedNames 목록에서 서버 저장 파일명을 하나씩 꺼내 storedName 변수에 담는다.
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
		
		// 기존 파일 삭제용 반복문
		for(NoticeFileDto deleteFile : deleteFiles) {
			// 실제 파일 삭제 처리
			try {
				boolean deleted = storageService.deleteFile(deleteFile.getFilePath(), deleteFile.getStoredName());
				
				if(!deleted) {
					// 어떤 파일 삭제가 실패했는지 보여줌
					log.error("첨부파일 정리 실패: " + deleteFile.getStoredName());
				}
			} catch (Exception e) {
				log.error("첨부파일 삭제 중 오류가 발생했습니다.", e);
			}
		}
		
		return updatedCount;
		
	}
	
	// 공지사항 삭제
	public int deleteNotice(long workspaceId, long noticeId) {
		
		return noticeMapper.deleteNotice(workspaceId, noticeId);
	}
	
	// 공지사항 다운로드 : 파일 한개 조회용
	public NoticeFileDto findWorkspaceFile(long workspaceId, long noticeId, long noticeFileId) throws Exception {
		
		NoticeFileDto noticeFile = noticeMapper.findWorkspaceFile(workspaceId, noticeId, noticeFileId);
		if(noticeFile == null) {
			throw new StorageFileNotFoundException("다운로드할 첨부파일이 없습니다.");
		}
		return noticeFile;
	}
}
