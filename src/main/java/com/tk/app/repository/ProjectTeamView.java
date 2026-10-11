package com.tk.app.repository;

public interface ProjectTeamView {
	Long getProjectMemberId();
    Long getWorkspaceMemberId();
    String getName();
    String getProjectName();
    Integer getRole();
    Integer getStatus();
}
