package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.domain.dto.ManageFileDto;

@Mapper
public interface ManageReportMapper {
	public List<ManageReportDto> listReport(Map<String, Object> map);
	public int countReport(Map<String, Object> map);
	public ManageReportDto reportDetail(Map<String, Object> map);

	public int countReviewButtonVisible(Map<String, Object> map);
    public int countRewardEligibleReviewer(Map<String, Object> map);
	public Long findReviewerProjectMemberId(Map<String, Object> map);
	public Long lockReport(Map<String, Object> map);
	public int insertReview(Map<String, Object> map);
	public int updateViewed(Map<String, Object> map);
	public int updateReview(Map<String, Object> map);

	public Long lockProjectProgress(Map<String, Object> map);
	public int updateTaskProgress(Map<String, Object> map);
	public int updateProjectProgress(Map<String, Object> map);

	public Map<String, Object> countReportStatuses(Map<String, Object> map);
	
	public int insertReport(ManageReportDto dto);
	public int insertReportFile(ManageFileDto dto);
	public List<Map<String, Object>> listWritableProjects(Map<String, Object> map) throws SQLException;
    public Long findOwnProjectMemberId(Map<String, Object> map);
    public int updateReport(ManageReportDto dto);
    public int deleteReportReviews(Map<String, Object> map);
    public int deleteReport(Map<String, Object> map);
    public int resetReview(Map<String, Object> map);
    public List<ManageFileDto> listReportFiles(Map<String, Object> map);
    public int deleteReportFile(Map<String, Object> map);
    public ManageReportDto selectPrevReport(Map<String, Object> map);
    public ManageReportDto selectNextReport(Map<String, Object> map);
}
