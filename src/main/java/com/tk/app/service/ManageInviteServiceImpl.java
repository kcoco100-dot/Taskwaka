package com.tk.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.ManageInviteDto;
import com.tk.app.mapper.ManageInviteMapper;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageInviteServiceImpl implements ManageInviteService {

    private final ManageInviteMapper mapper;
    private final ManageWorkspaceInviteService invites;
    
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // 워크스페이스 번호 조회
    @Override
    public Long getWorkspaceIdByDomainKey(Map<String, Object> map) {
        Long workspaceId = null;
        try {
            String domainKey = (String) map.get("domainKey");
            workspaceId = mapper.selectWorkspaceIdByDomainKey(domainKey);
        } catch (Exception e) {
            log.info("getWorkspaceIdByDomainKey : ", e);
        }
        return workspaceId;
    }

    // 초대 대상 조회
    @Override
    public List<ManageInviteDto> getEligibleMembers(Map<String, Object> map) {
        List<ManageInviteDto> list = null;
        try {
            list = mapper.selectEligibleMembers(map);
        } catch (Exception e) {
            log.info("getEligibleMembers : ", e);
        }
        return list;
    }

    @Override
    public void sendInvites(Map<String, Object> map) throws Exception {
        if (!invites.sendInvitesAndMail(map).isEmpty()) throw new IllegalStateException("일부 초대 발송에 실패했습니다.");
    }

    @Override
    public void acceptInvite(Map<String, Object> map) throws Exception {
        invites.acceptInvite(map);
    }
}
