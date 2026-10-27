package com.tk.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface LoginHistoryMapper {
	public int insertLoginHistory(@Param("member_id") long member_id);
	public int countLoginHistory();
	public int countDistinctMembers();
	public int countDailyLoginHistory();
	public int countDailyDistinctMembers();
	public int countWeekLogin();
	public int countWeekMember();
	public int countMonthLogin();
	public int countMonthMember();
	public int countYearLogin();
	public int countYearMember();
}
