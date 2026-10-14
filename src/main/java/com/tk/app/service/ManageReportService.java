package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageReportDto;

public interface ManageReportService {
	public List<ManageReportDto> listReport(Map<String, Object> map);
	public int countReport(Map<String, Object> map);
	public List<ManageReportDto> reportDetail(Map<String, Object> map);

	public int updateReview(Map<String, Object> map);
	public int insertProjectReport(ManageReportDto dto);
	public int insertReview(ManageReportDto dto);
}
