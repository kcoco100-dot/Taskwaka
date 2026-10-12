package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.WorkspaceDto;

@Mapper
public interface WorkspaceMapper {
	
    public WorkspaceDto findAccessibleWorkspace(
            @Param("domain") String domain,
            @Param("memberId") long memberId);
    
    // 워크 목록 조회
    // Main 은 워크를 선택하기 전 화면이므로 회원번호만 필요
    public List<WorkspaceDto> findWorkspaceList(
    			@Param("memberId") long memberId);
}