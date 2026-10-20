package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageReportDto;

public interface ManageReportService {
    public List<ManageReportDto> listReport(Map<String, Object> map);
    public int countReport(Map<String, Object> map);
    public ManageReportDto openReport(Map<String, Object> map);
    public void updateReview(Map<String, Object> map);
    public Map<String, Object> countReportStatuses(Map<String, Object> map);
}
