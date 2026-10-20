package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageNoticeDto;

public interface ManageNoticeService {
	
	public List<ManageNoticeDto> selectNoticeList(Map<String, Object> map) throws SQLException;
    public ManageNoticeDto selectNoticeDetail(long notice_id) throws SQLException;
    public void updateViewCount(long notice_id) throws SQLException;
    
    public void insertNotice(ManageNoticeDto dto) throws SQLException;
    public void insertNoticeFile(ManageNoticeDto dto) throws SQLException;
    public void updateNotice(ManageNoticeDto dto) throws SQLException;
    public void deleteNoticeFile(long notice_id) throws SQLException;
    
    public int deleteNotice(Map<String, Object> map) throws SQLException;
    
    public int dataCount(Map<String, Object> map) throws SQLException;
}
