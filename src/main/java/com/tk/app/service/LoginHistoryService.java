package com.tk.app.service;

public interface LoginHistoryService {
	// 로그인 기록 추가
	public int insertLoginHistory(long member_id);
	// 로그인 횟수 카운츠
	public int countLoginHistory();
	// 접속 회원 (중복 x)
	public int countDistinctMembers();
	// 일별 로그인 성공 횟수
	public int countDailyLoginHistory();
	// 일별 로그인 성공 횟수 (중복 x)
	public int countDailyDistinctMembers();
	// 주간 로그인 성공 횟수
	public int countWeekLogin();
	// 주간 접속 회원 수(중복 제외)
	public int countWeekMember();




}
