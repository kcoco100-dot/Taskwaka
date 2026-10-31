package com.tk.app.domain.dto;

import com.tk.app.common.MyUtil;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class NoticeDto {
	private Long noticeId; // 공지번호
	private String title;
	private Long workspaceId; // 워크 번호
	private Long memberId; // 회원 번호
	
	private String authorName; // 작성자 이름
	private String content; // 공지 내용
	
	private String createdAt; // 등록일
	private Long viewCount; // 조회수
	
    public void setContent(String content) {
        this.content = MyUtil.sanitize(content);
    }
}
