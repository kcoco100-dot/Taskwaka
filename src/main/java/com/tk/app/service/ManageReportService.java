package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.domain.dto.ManageFileDto;
import org.springframework.web.multipart.MultipartFile;

public interface ManageReportService {
    public List<ManageReportDto> listReport(Map<String, Object> map);
    public int countReport(Map<String, Object> map);
    public ManageReportDto openReport(Map<String, Object> map);
    public void updateReview(Map<String, Object> map);
    public Map<String, Object> countReportStatuses(Map<String, Object> map);
    public List<Map<String, Object>> listWritableProjects(Map<String, Object> map) throws Exception;
    
    public void createReport(ManageReportDto dto, Map<String, Object> map, List<MultipartFile> files) throws Exception;
	public void editReport(ManageReportDto dto, Map<String, Object> map, List<MultipartFile> files, List<Long> deleteFileIds) throws Exception;
    public void deleteReport(Map<String, Object> map) throws Exception;
    public ManageReportDto getReport(Map<String, Object> map);
    
    public List<ManageFileDto> listReportFiles(Map<String, Object> map);
    
    public ManageReportDto selectPrevReport(Map<String, Object> map);
    public ManageReportDto selectNextReport(Map<String, Object> map);
    
}
