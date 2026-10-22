package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageNoticeDto;

@Mapper
public interface ManageNoticeMapper {
	public int insertNotice(ManageNoticeDto dto) throws SQLException;
	public int updateNotice(ManageNoticeDto dto) throws SQLException;
	public int deleteNotice(Map<String, Object> map) throws SQLException;
	
    public List<ManageNoticeDto> selectNoticeList(Map<String, Object> map) throws SQLException;
    public int dataCount(Map<String, Object> map) throws SQLException;
    public int updateViewCount(Map<String, Object> map) throws SQLException;
    
    public ManageNoticeDto selectNoticeDetail(Map<String, Object> map) throws SQLException;
    public ManageNoticeDto selectPrevNotice(Map<String, Object> map) throws SQLException;
    public ManageNoticeDto selectNextNotice(Map<String, Object> map) throws SQLException;

    public ManageNoticeDto selectNoticeFile(Map<String, Object> map) throws SQLException;
    public List<ManageNoticeDto> selectNoticeFiles(Map<String, Object> map) throws SQLException;

    public int insertNoticeFile(ManageNoticeDto dto) throws SQLException;
    public int deleteNoticeFile(Map<String, Object> map) throws SQLException;
}
