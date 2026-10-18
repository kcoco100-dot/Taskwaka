
package com.tk.app.admin.service;

import java.util.List;

import com.tk.app.admin.domain.dto.AdminWorkspaceDto;

public interface AdminWorkspaceService {

    public List<AdminWorkspaceDto> findAllWorkspaces();
    public int updateWorkspaceStatus(AdminWorkspaceDto dto);
}
