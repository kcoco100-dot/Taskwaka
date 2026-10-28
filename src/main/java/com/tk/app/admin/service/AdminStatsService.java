package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.AdminMonthlyRevenueDto;

public interface AdminStatsService {

	// 결제 횟수
	public int countPayment();
	// 합친 결제 금액
	public long sumPayment();
	// 환불 금액
	public long sumRefund();
	// 월별 결제 횟수
	public int countMonthPayment();
	// 월별 결제 금액
	public long sumMonthPayment();
	// 월별 환불 금액
	public long sumMonthRefund();
	// 분기 결제 횟수
	public int countQuarterPayment();
	// 분기 결제 금액
	public long sumQuarterPayment();
	// 분기 환불 금액
	public long sumQuarterRefund();
	// 
	public List<AdminMonthlyRevenueDto> listMonthlyRevenue();

}
