package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageNoticeDto;

@Mapper
public interface ManageNoticeMapper {
	
	public List<ManageNoticeDto> selectNoticeList(Map<String, Object> map) throws SQLException;
    public ManageNoticeDto selectNoticeDetail(long notice_id) throws SQLException;
    public void updateViewCount(long notice_id) throws SQLException;
    
    public void insertNotice(ManageNoticeDto dto) throws SQLException;
    public void insertNoticeFile(ManageNoticeDto dto) throws SQLException;
    public int updateNotice(ManageNoticeDto dto) throws SQLException;
    public void deleteNoticeFile(long notice_id) throws SQLException;
    
    public int deleteNotice(Map<String, Object> map) throws SQLException;
    
    public int dataCount(Map<String, Object> map) throws SQLException;
}
