package com.tk.app.repository;

import java.sql.Clob;

// 회의록 목록에 필요한 값을 받는 인터페이스
public interface MeetingMinuteListView {
	Long getMeetingMinuteId(); // 회의록 번호
	String getTitle(); // 회의록 제목
	Clob getSummary(); // 제목 아래에 표시할 본문
	String getMeetingDate(); // 회의일
	String getAuthorName(); // 회의록 작성자 이름
}
