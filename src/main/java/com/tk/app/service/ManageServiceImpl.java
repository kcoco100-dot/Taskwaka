package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.common.MyUtil;
import com.tk.app.domain.dto.ManageDto;
import com.tk.app.mapper.ManageMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageServiceImpl implements ManageService{
	private final ManageMapper mapper;
	private final MyUtil myUtil;
	
	@Override
	public void insertProject(ManageDto dto) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateProject(ManageDto dto) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteProject(ManageDto dto) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException {
		List<ManageDto> list = null;
		
		try {
			list = mapper.listProject(map);
			
		} catch (Exception e) {
			log.info("listProject : ", e);
			
			throw e;
		}
		
		return list;
	}

	@Override
	public int dataCount(Map<String, Object> map) throws Exception {
		try {
			return mapper.dataCount(map);
        } catch (Exception e) {
        	log.info("dataCount : ", e);

        	throw e;
        }
	}

}
