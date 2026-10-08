package com.tk.app.admin.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceDto {
    private long workspaceId;
    private long memberId;
    private String subject;
    private int status;
}