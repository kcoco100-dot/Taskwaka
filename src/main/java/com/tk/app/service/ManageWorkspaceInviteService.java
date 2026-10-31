package com.tk.app.service;

import java.util.List;
import java.util.Map;
import com.tk.app.domain.dto.ManageInviteDto;

public interface ManageWorkspaceInviteService {
    List<Long> sendInvitesAndMail(Map<String, Object> map) throws Exception;
    List<ManageInviteDto> getSearchEligibleMembers(Map<String, Object> map);
    void acceptInvite(Map<String, Object> map) throws Exception;
}