package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.common.MyUtil;
import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.ManageNoticeDto;
import com.tk.app.mapper.ManageNoticeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageNoticeServiceImpl implements ManageNoticeService {
	
	private final ManageNoticeMapper mapper;
	private final StorageService storageService;
	private final MyUtil myUtil;


	@Override
	public List<ManageNoticeDto> selectNoticeList(Map<String, Object> map) throws SQLException {
		List<ManageNoticeDto> list = null;
		try {
			list = mapper.selectNoticeList(map);
		} catch (Exception e) {
			log.info("selectNoticeList : ", e);
			
			throw e;
		}
		return list;
	}

	@Override
	public int dataCount(Map<String, Object> map) throws SQLException {
		int result = 0;
		try {
			result = mapper.dataCount(map);
		} catch (Exception e) {
			log.info("dataCount : ", e);
		}
		return result;
	}

	@Override
	public ManageNoticeDto selectNoticeDetail(long notice_id) throws SQLException {
		ManageNoticeDto dto = null;
		
		try {
			dto = mapper.selectNoticeDetail(notice_id);
		} catch (Exception e) {
			log.info("selectNoticeDetail ", e);
			
			throw e;
		}
		
		return dto;
	}

	@Override
	public void updateViewCount(long notice_id) throws SQLException {
		try {
			mapper.updateViewCount(notice_id);
		} catch (Exception e) {
			log.info("updateViewCount ", e);
			
			throw e;
		}
		
	}

	@Override
	public void insertNotice(ManageNoticeDto dto) throws SQLException {
		try {
			mapper.insertNotice(dto);
		} catch (Exception e) {
			log.info("insertNotice ", e);
			
			throw e;
		}
		
	}

	@Override
	public void insertNoticeFile(ManageNoticeDto dto) throws SQLException {
		try {
			mapper.insertNoticeFile(dto);
		} catch (Exception e) {
			log.info("insertNoticeFile ", e);
			
			throw e;
		}
		
	}

	@Override
	public void updateNotice(ManageNoticeDto dto) throws SQLException {
		try {
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}

	@Override
	public void deleteNoticeFile(long notice_id) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ManageNoticeDto> deleteNotice(Map<String, Object> map) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}



}
