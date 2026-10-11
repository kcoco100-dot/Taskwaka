package com.tk.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tk.app.domain.entity.ManageProjectMember;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ManageProjectMember, Long> {

	@Query(value = 
	        "SELECT " +
	        "  pm.project_member_id AS projectMemberId, " +
	        "  wm.workspace_member_id AS workspaceMemberId, " +
	        "  md.name AS name, " +
	        "  p.subject AS projectName, " +
	        "  pm.role AS role, " +
	        "  wm.status AS status " +
	        "FROM workspace_members wm " +
	        "JOIN member_details md ON wm.member_id = md.member_id " +
	        "LEFT JOIN project_members pm ON wm.workspace_member_id = pm.workspace_member_id " +
	        "LEFT JOIN projects p ON pm.project_id = p.project_id " +
	        "WHERE wm.workspace_id = :workspaceId " +
	        "ORDER BY md.name ASC, p.project_id DESC", 
	        nativeQuery = true)
	    List<ProjectTeamView> findWorkspaceMembersWithProject(@Param("workspaceId") Long workspaceId);
}
