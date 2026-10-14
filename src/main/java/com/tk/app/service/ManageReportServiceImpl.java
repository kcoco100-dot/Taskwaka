package com.tk.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.mapper.ManageReportMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageReportServiceImpl implements ManageReportService {

	private final ManageReportMapper mapper;
	
	@Override
	public List<ManageReportDto> listReport(Map<String, Object> map) {
		List<ManageReportDto> list = null;
		try {
			list = mapper.listReport(map);
		} catch (Exception e) {
			log.info("listReport : ",e);
		}
		return list;
	}

	@Override
	public int countReport(Map<String, Object> map) {
		int result = 0;
		try {
			result = mapper.countReport(map);
		} catch (Exception e) {
			log.info("countReport : ",e);
		}
		return result;
	}

	@Override
	public List<ManageReportDto> reportDetail(Map<String, Object> map) {
		List<ManageReportDto> list = null;
		try {
			list = mapper.reportDetail(map);
		} catch (Exception e) {
			log.info("ReportDetail : ",e);
		}
		return list;
	}

	@Override
	public int updateReview(Map<String, Object> map) {
		int result = 0;
		try {
			result = mapper.updateReview(map);
		} catch (Exception e) {
			log.info("updateReview : ",e);
		}
		return result;
	}

	@Override
	public int insertProjectReport(ManageReportDto dto) {
		int result = 0;
		try {
			result = mapper.insertProjectReport(dto);
		} catch (Exception e) {
			log.info("insertProjectReport : ",e);
		}
		return result;
	}

	@Override
	public int insertReview(ManageReportDto dto) {
		int result = 0;
		try {
			result = mapper.insertReview(dto);
		} catch (Exception e) {
			log.info("insertReview : ",e);
		}
		return result;
	}

}
