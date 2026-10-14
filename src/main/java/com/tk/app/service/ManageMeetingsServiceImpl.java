package com.tk.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.ManageMeetingsDto;
import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.mapper.ManageMeetingsMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageMeetingsServiceImpl implements ManageMeetingsService {

	private final ManageMeetingsMapper mapper;
	
	@Override
	public List<ManageMeetingsDto> findMeetingList(Map<String, Object> map) {
		List<ManageMeetingsDto> list = null;
		
		try {
			list = mapper.findMeetingList(map);
		} catch (Exception e) {
			log.info("findMeetingList : ", e);
		}
		
		return list;
	}

	@Override
	public int countMeetings(Map<String, Object> map) {
		int result = 0;
		try {
			result = mapper.countMeetings(map);
		} catch (Exception e) {
			log.info("countMeetings : ", e);
		}
		return result;
	}

	@Override
	public List<ManageMeetingsDto> findMeetingsById(Map<String, Object> map) {
		List<ManageMeetingsDto> list = null;
		
		try {
			list = mapper.findMeetingsById(map);
		} catch (Exception e) {
			log.info("findMeetingsById : ", e);
		}
		return list;
	}

	@Override
	public List<ManageMeetingsDto> updateMeetings(Map<String, Object> map) {
		List<ManageMeetingsDto> list = null;
		
		try {
			list = mapper.updateMeetings(map);
		} catch (Exception e) {
			log.info("updateMeetings : ", e);
		}
		return list;
	}

	@Override
	public List<ManageMeetingsDto> deleteMeetings(Map<String, Object> map) {
		List<ManageMeetingsDto> list = null;
		
		try {
			list = mapper.deleteMeetings(map);
		} catch (Exception e) {
			log.info("deleteMeetings : ", e);
		}
		return list;
	}

}
