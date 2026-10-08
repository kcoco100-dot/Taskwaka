package com.tk.app.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.admin.domain.dto.WorkspaceDto;

@Mapper
public interface WorkspaceMapper {
    WorkspaceDto findAccessibleWorkspace(
            @Param("domain") String domain,
            @Param("memberId") long memberId);
}