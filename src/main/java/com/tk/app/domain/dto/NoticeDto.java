package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class NoticeDto {
	private Long noticeId; // 공지번호
	private String title;
	
	private String authorName; // 작성자 이름
	
	private String createdAt; // 등록일
	private Long viewCount; // 조회수
	
}
