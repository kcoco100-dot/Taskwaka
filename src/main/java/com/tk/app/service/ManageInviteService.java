package com.tk.app.service;

import java.util.List;
import java.util.Map;

import com.tk.app.domain.dto.ManageInviteDto;

public interface ManageInviteService {
    
    public Long getWorkspaceIdByDomainKey(Map<String, Object> map);
    public List<ManageInviteDto> getEligibleMembers(Map<String, Object> map);
    public void sendInvites(Map<String, Object> map) throws Exception;
    public void acceptInvite(Map<String, Object> map) throws Exception;
}