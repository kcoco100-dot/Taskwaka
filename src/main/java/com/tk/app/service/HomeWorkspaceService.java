package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.HomeWorkspaceDto;
import com.tk.app.mapper.HomeWorkspaceMapper;

@Service
public class HomeWorkspaceService {

    private final HomeWorkspaceMapper homeWorkspaceMapper;

    public HomeWorkspaceService(HomeWorkspaceMapper homeWorkspaceMapper) {
        this.homeWorkspaceMapper = homeWorkspaceMapper;
    }

    // 로그인 회원의 참여 워크 목록 조회
    public List<HomeWorkspaceDto> findWorkspaceList(long memberId) {
        return homeWorkspaceMapper.findWorkspaceList(memberId);
    }
}
