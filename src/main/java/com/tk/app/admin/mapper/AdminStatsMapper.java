package com.tk.app.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.AdminMonthlyRevenueDto;

@Mapper
public interface AdminStatsMapper {

	public int countPayment();
	public long sumPayment();
	public long sumRefund();

	public int countMonthPayment();
	public long sumMonthPayment();
	public long sumMonthRefund();
	
	public int countQuarterPayment();
	public long sumQuarterPayment();
	public long sumQuarterRefund();
	
	public List<AdminMonthlyRevenueDto> listMonthlyRevenue();
	public List<AdminMonthlyRevenueDto> listDateRevenue(@Param("startDate") String startDate, @Param("endDate") String endDate);
	
	public int countDatePayment(@Param("startDate") String startDate, @Param("endDate") String endDate);
	public long sumDatePayment(@Param("startDate") String startDate, @Param("endDate") String endDate);
	public long sumDateRefund(@Param("startDate") String startDate, @Param("endDate") String endDate);

}
