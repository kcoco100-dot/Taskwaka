package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.tk.app.domain.dto.HomeWorkspaceDto;

@Mapper
public interface HomeWorkspaceMapper {

    List<HomeWorkspaceDto> findWorkspaceList(
            @Param("memberId") long memberId);
}