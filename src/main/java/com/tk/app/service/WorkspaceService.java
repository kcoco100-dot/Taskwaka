
package com.tk.app.service;

import org.springframework.stereotype.Service;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.mapper.WorkspaceMapper;

@Service
public class WorkspaceService {

    private final WorkspaceMapper workspaceMapper;

    public WorkspaceService(WorkspaceMapper workspaceMapper) {
        this.workspaceMapper = workspaceMapper;
    }

    public WorkspaceDto findAccessibleWorkspace(
            String domainKey, long memberId) {

        return workspaceMapper.findAccessibleWorkspace(
                domainKey, memberId);
    }
}
