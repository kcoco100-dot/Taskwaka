package com.tk.app.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageReportDto;

@Mapper
public interface ManageReportMapper {
	public List<ManageReportDto> listReport(Map<String, Object> map);
	public int countReport(Map<String, Object> map);
	public List<ManageReportDto> reportDetail(Map<String, Object> map);

	public int updateReview(Map<String, Object> map);
	public int insertProjectReport(ManageReportDto dto);
	public int insertReview(ManageReportDto dto);
}
