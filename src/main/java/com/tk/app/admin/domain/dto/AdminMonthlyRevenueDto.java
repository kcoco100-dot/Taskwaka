package com.tk.app.admin.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminMonthlyRevenueDto {
	private String month;
	private long payment_amount;
	private long refund_amount;
	private long net_amount;
}
