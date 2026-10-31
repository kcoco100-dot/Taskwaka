package com.tk.app.domain.dto;

public record ProjectActor(long memberId, long workspaceId, long workspaceMemberId, long projectMemberId, int role) {
    public boolean isLeader() {
        return role >= 30;
    }
}
