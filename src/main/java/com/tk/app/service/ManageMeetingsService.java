package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageMeetingsDto;
import com.tk.app.domain.dto.MeetingMinutesDto;

public interface ManageMeetingsService {
	public List<ManageMeetingsDto> findMeetingList(Map<String, Object> map);
	public int countMeetings(Map<String, Object> map);
	public List<ManageMeetingsDto> findMeetingsById(Map<String, Object> map);
	public List<ManageMeetingsDto> updateMeetings(Map<String, Object> map);
	public List<ManageMeetingsDto> deleteMeetings(Map<String, Object> map);
}
