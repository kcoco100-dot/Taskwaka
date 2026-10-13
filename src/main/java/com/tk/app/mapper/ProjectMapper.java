package com.tk.app.mapper;

import com.tk.app.domain.dto.ProjectDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProjectMapper {
    /**
     * 프로젝트 ID로 프로젝트 전체 정보 단건 조회
     *
     * @param projectId 조회할 프로젝트 번호
     * @return 프로젝트 전체 상세 정보 DTO
     */
    ProjectDto selectProjectById(@Param("projectId") Long projectId);
}
