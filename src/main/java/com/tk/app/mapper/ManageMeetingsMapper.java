package com.tk.app.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageMeetingsDto;

@Mapper
public interface ManageMeetingsMapper {
	public List<ManageMeetingsDto> findMeetingList(Map<String, Object> map);
	public int countMeetings(Map<String, Object> map);
	public List<ManageMeetingsDto> findMeetingsById(Map<String, Object> map);
	public int updateMeetings(Map<String, Object> map);
	public int deleteMeetings(Map<String, Object> map);
}
