package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NoticeFileDto {
	private Long noticeFileId; // 공지 파일 번호
	private Long noticeId; // 공지번호
	private String originalName; // 사용자가 올린 원본 파일명
	private String storedName; // 서버 파일명
	private String filePath; // 서버에 저장되는 파일 경로
	private Long fileSize; // 파일 크기
}
