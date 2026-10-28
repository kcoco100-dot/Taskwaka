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
	// 최근 6개월 월별 결제, 환불, 순결제 금액
	public List<AdminMonthlyRevenueDto> listMonthlyRevenue();
	// 선택 기간 월별 결제, 환불, 순결제 금액
	public List<AdminMonthlyRevenueDto> listDateRevenue(String startDate, String endDate);
	// 선택 기간 결제 성공 건수
	public int countDatePayment(String startDate, String endDate);
	// 선택 기간 결제 완료 금액 합계
	public long sumDatePayment(String startDate, String endDate);
	// 선택 기간 환불 완료 금액 합계
	public long sumDateRefund(String startDate, String endDate);
	


}
