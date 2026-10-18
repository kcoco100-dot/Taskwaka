
package com.tk.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.AdminWorkspaceDto;
import com.tk.app.admin.mapper.AdminWorkspaceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminWorkspaceServiceImpl implements AdminWorkspaceService {

    private final AdminWorkspaceMapper mapper;

    @Override
    public List<AdminWorkspaceDto> findAllWorkspaces() {
        try {
            return mapper.findAllWorkspaces();
        } catch (Exception e) {
            log.error("워크 목록 조회 실패", e);
            throw e;
        }
    }

    @Override
    public int updateWorkspaceStatus(AdminWorkspaceDto dto) {
        try {
            return mapper.updateWorkspaceStatus(dto);

        } catch (Exception e) {
            log.error("워크 상태 변경 실패", e);
            throw e;
        }
    }

}
