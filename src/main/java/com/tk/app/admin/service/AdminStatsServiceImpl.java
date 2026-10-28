package com.tk.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.AdminMonthlyRevenueDto;
import com.tk.app.admin.mapper.AdminStatsMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStatsServiceImpl implements AdminStatsService{

	private final AdminStatsMapper adminStatsMapper;
	
	@Override
	public int countPayment() {
		try {
			return adminStatsMapper.countPayment();
		} catch (Exception e) {
			log.error("countPayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumPayment() {
		try {
			return adminStatsMapper.sumPayment();
		} catch (Exception e) {
			log.error("sumPayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumRefund() {
		try {
			return adminStatsMapper.sumRefund();
		} catch (Exception e) {
			log.error("sumRefund : ", e);
			throw e;
		}
	}

	@Override
	public int countMonthPayment() {
		try {
			return adminStatsMapper.countMonthPayment();
		} catch (Exception e) {
			log.error("countMonthPayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumMonthPayment() {
		try {
			return adminStatsMapper.sumMonthPayment();
		} catch (Exception e) {
			log.error("sumMonthPayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumMonthRefund() {
		try {
			return adminStatsMapper.sumMonthRefund();
		} catch (Exception e) {
			log.error("sumMonthRefund : ", e);
			throw e;
		}
	}

	@Override
	public int countQuarterPayment() {
		try {
			return adminStatsMapper.countQuarterPayment();
		} catch (Exception e) {
			log.error("countQuarterPayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumQuarterPayment() {
		try {
			return adminStatsMapper.sumQuarterPayment();
		} catch (Exception e) {
			log.error("sumQuarterPayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumQuarterRefund() {
		try {
			return adminStatsMapper.sumQuarterRefund();
		} catch (Exception e) {
			log.error("sumQuarterRefund : ", e);
			throw e;
		}
	}

	@Override
	public List<AdminMonthlyRevenueDto> listMonthlyRevenue() {
		try {
			return adminStatsMapper.listMonthlyRevenue();
		} catch (Exception e) {
			log.error("listMonthlyRevenue : ", e);
			throw e;
		}
	}

	@Override
	public int countDatePayment(String startDate, String endDate) {
		try {
			return adminStatsMapper.countDatePayment(startDate, endDate);
		} catch (Exception e) {
			log.error("countDatePayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumDatePayment(String startDate, String endDate) {
		try {
			return adminStatsMapper.sumDatePayment(startDate, endDate);
		} catch (Exception e) {
			log.error("sumDatePayment : ", e);
			throw e;
		}
	}

	@Override
	public long sumDateRefund(String startDate, String endDate) {
		try {
			return adminStatsMapper.sumDateRefund(startDate, endDate);
		} catch (Exception e) {
			log.error("sumDateRefund : ", e);
			throw e;
		}
	}

	@Override
	public List<AdminMonthlyRevenueDto> listDateRevenue(String startDate, String endDate) {
		try {
			return adminStatsMapper.listDateRevenue(startDate, endDate);
		} catch (Exception e) {
			log.error("listDateRevenue : ", e);
			throw e;
		}
	}
	
}
