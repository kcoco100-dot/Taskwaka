package com.tk.app.admin.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.AdminMemberDto;
import com.tk.app.admin.domain.dto.AdminWorkspaceMemberDto;
import com.tk.app.admin.mapper.AdminMemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMemberServiceImpl implements AdminMemberService {
	private final AdminMemberMapper adminMemberMapper;
	

	@Override
	public List<AdminMemberDto> findAllMembers() {
	    try {
	        return adminMemberMapper.findAllMembers();
	    } catch (Exception e) {
	        log.error("전체 회원 조회 실패", e);
	        throw e;
	    }
	}
	
	@Override
	public List<AdminWorkspaceMemberDto> findMemberWorkspaces(long member_id) {
	    try {
	        return adminMemberMapper.findMemberWorkspaces(member_id);
	    } catch (Exception e) {
	        log.error("회원 소속 워크 조회 실패 - 회원번호: {}", member_id, e);
	        throw e;
	    }
	}
	
	@Override
	public AdminMemberDto findMemberById(long member_id) {
	    try {
	        return adminMemberMapper.findMemberById(member_id);
	    } catch (Exception e) {
	        log.error("회원 상세 조회 실패 - 회원번호: {}", member_id, e);
	        throw e;
	    }
	}
	
	@Override
	public List<AdminMemberDto> searchMembers(String keyword) {
	    try {
	        return adminMemberMapper.searchMembers(keyword);
	    } catch (Exception e) {
	        log.error("회원 검색 실패 - 검색어: {}", keyword, e);
	        throw e;
	    }
	}
	
	@Override
	public List<AdminMemberDto> searchMembersByStatus(String keyword, Integer status) {
	    try {
	        return adminMemberMapper.searchMembersByStatus(keyword, status);
	    } catch (Exception e) {
	        log.error("회원 조건 검색 실패 - 상태: {}", status, e);
	        throw e;
	    }
	}
	
	@Override
	public int updateMemberStatus(long member_id, int status) {
	    try {
	        return adminMemberMapper.updateMemberStatus(member_id, status);
	    } catch (Exception e) {
	        log.error("회원 상태 변경 실패 - 회원번호: {}, 상태: {}",
	                  member_id, status, e);
	        throw e;
	    }
	}

	@Override
	public int dataCountMembers(Map<String, Object> map) {
	    try {
	        return adminMemberMapper.dataCountMembers(map);
	    } catch (Exception e) {
	        log.error("회원 수 조회 실패", e);
	        throw e;
	    }
	}

	@Override
	public List<AdminMemberDto> listMembersByPage(Map<String, Object> map) {
	    try {
	        return adminMemberMapper.listMembersByPage(map);
	    } catch (Exception e) {
	        log.error("회원 페이지 조회 실패", e);
	        throw e;
	    }
	}
	
	
}
