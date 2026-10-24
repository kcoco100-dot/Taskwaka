package com.tk.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.ManageMeetingsDto;
import com.tk.app.mapper.ManageMeetingsMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageMeetingsServiceImpl implements ManageMeetingsService {

	private final ManageMeetingsMapper mapper;

	// 회의록 목록 조회
	@Override
	public List<ManageMeetingsDto> findMeetingList(Map<String, Object> map) {
		List<ManageMeetingsDto> list = new java.util.ArrayList<>();

		try {
			list = mapper.findMeetingList(map);
		} catch (Exception e) {
			log.info("findMeetingList : ", e);
            throw e;
		}

		return list;
	}

	// 회의록 건수 조회
	@Override
	public int countMeetings(Map<String, Object> map) {
		int result = 0;
		try {
			result = mapper.countMeetings(map);
		} catch (Exception e) {
			log.info("countMeetings : ", e);
            throw e;
		}
		return result;
	}

	// 회의록 상세 조회
	@Override
	public List<ManageMeetingsDto> findMeetingsById(Map<String, Object> map) {
		List<ManageMeetingsDto> list = new java.util.ArrayList<>();

		try {
			list = mapper.findMeetingsById(map);

		} catch (Exception e) {
			log.info("findMeetingsById : ", e);
            throw e;
		}
		return list;
	}

	// 회의록 수정
	@Override
	public int updateMeetings(Map<String, Object> map) {
        int result = 0;
		try {
			result = mapper.updateMeetings(map);
		} catch (Exception e) {
			log.info("updateMeetings : ", e);
			throw e;
		}
        return result;
	}

	// 회의록 숨김
	@Override
	public int deleteMeetings(Map<String, Object> map) {
        int result = 0;
		try {
			result = mapper.deleteMeetings(map);
		} catch (Exception e) {
			log.info("deleteMeetings : ", e);
			throw e;
		}
        return result;
	}
    // 이전 회의록 조회
    @Override
    public ManageMeetingsDto selectPrevMeeting(Map<String, Object> map) {
        ManageMeetingsDto result = null;
        try {
            result = mapper.selectPrevMeeting(map);
        } catch (Exception e) {
            log.info("selectPrevMeeting : ", e);
            throw e;
        }
        return result;
    }
    // 다음 회의록 조회
    @Override
    public ManageMeetingsDto selectNextMeeting(Map<String, Object> map) {
        ManageMeetingsDto result = null;
        try {
            result = mapper.selectNextMeeting(map);
        } catch (Exception e) {
            log.info("selectNextMeeting : ", e);
            throw e;
        }
        return result;
    }
}
