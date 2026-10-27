package com.tk.app.service;

import org.springframework.stereotype.Service;

import com.tk.app.mapper.LoginHistoryMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryServiceImpl implements LoginHistoryService {
	
	private final LoginHistoryMapper loginHistoryMapper;

	@Override
	public int insertLoginHistory(long member_id) {
		try {
			return loginHistoryMapper.insertLoginHistory(member_id);
		} catch (Exception e) {
			log.error("insertLoginHistory : ", e);
			throw e;
		}
	}

	@Override
	public int countLoginHistory() {
		try {
			return loginHistoryMapper.countLoginHistory();
		} catch (Exception e) {
			log.error("countLoginHistory : ", e);
			throw e;
		}
	}

	@Override
	public int countDistinctMembers() {
		try {
			return loginHistoryMapper.countDistinctMembers();
		} catch (Exception e) {
			log.error("countDistinctMembers : ", e);
			throw e;
		}
	}

	@Override
	public int countDailyLoginHistory() {
		try {
			return loginHistoryMapper.countDailyLoginHistory();
		} catch (Exception e) {
			log.error("countDailyLoginHistory : ", e);
			throw e;
		}
	}

	@Override
	public int countDailyDistinctMembers() {
		try {
			return loginHistoryMapper.countDailyDistinctMembers();
		} catch (Exception e) {
			log.error("countDailyDistinctMembers : ", e);
			throw e;
		}
	}

	@Override
	public int countWeekLogin() {
		try {
			return loginHistoryMapper.countWeekLogin();
		} catch (Exception e) {
			log.error("countWeekLogin : ", e);
			throw e;
		}
	}

	@Override
	public int countWeekMember() {
		try {
			return loginHistoryMapper.countWeekMember();
		} catch (Exception e) {
			log.error("countWeekMember : ", e);
			throw e;
		}
	}
	
}
