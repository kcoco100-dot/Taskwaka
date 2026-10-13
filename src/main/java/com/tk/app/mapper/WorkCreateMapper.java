package com.tk.app.mapper;

import java.sql.SQLException;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.WorkCreateDto;

@Mapper
public interface WorkCreateMapper {
	public void insertWork(WorkCreateDto dto) throws SQLException;
	public void insertWorkPM(WorkCreateDto dto) throws SQLException;
	public void insertDomainKey(WorkCreateDto dto) throws SQLException;
	
	// 도메인 주소 중복 확인
	public int countByDomainKey(String domainKey);
	
	public Long findMemberIdByLoginId(String loginId);
	
}
