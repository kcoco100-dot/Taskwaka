package com.tk.app.repository;

public interface ProjectTeamView {
	Long getProjectMemberId();
    Long getWorkspaceMemberId();
    Long getProjectId();
    
    String getName();
    String getProjectName();
    Integer getRole();
    Integer getStatus();
}
