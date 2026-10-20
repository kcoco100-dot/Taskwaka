package com.tk.app.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageReportDto;

@Mapper
public interface ManageReportMapper {
    public List<ManageReportDto> listReport(Map<String, Object> map);
    public int countReport(Map<String, Object> map);
    public ManageReportDto reportDetail(Map<String, Object> map);

    public Long findReviewerProjectMemberId(Map<String, Object> map);
    public Long lockReport(Map<String, Object> map);
    public int insertReview(Map<String, Object> map);
    public int updateViewed(Map<String, Object> map);
    public int updateReview(Map<String, Object> map);
    
    public Long lockProjectProgress(Map<String, Object> map);
    public int updateTaskProgress(Map<String, Object> map);
    public int updateProjectProgress(Map<String, Object> map);
    
    public Map<String, Object> countReportStatuses(Map<String, Object> map);
}
