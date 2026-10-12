package com.tk.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import com.tk.app.domain.entity.ManageProjectMember;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectMemberRepository extends JpaRepository<ManageProjectMember, Long> {

	@Query(value = 
	        "SELECT " +
	        "   wm.workspace_member_id AS workspaceMemberId, " +
	        "   md.name AS name, " +
	        "   wm.status AS status, " +
	        "   pm.project_member_id AS projectMemberId, " +
	        "   pm.project_id AS projectId, " +
	        "   p.subject AS projectName, " +
	        "   pm.role AS role " +
	        "FROM workspace_members wm " +
	        "JOIN member_details md ON wm.member_id = md.member_id " +
	        "LEFT JOIN project_members pm ON wm.workspace_member_id = pm.workspace_member_id AND pm.status = 0 " +
	        "LEFT JOIN projects p ON pm.project_id = p.project_id " +
	        "WHERE wm.workspace_id = :workspaceId AND wm.status = 0 " +
	        "ORDER BY md.name ASC", 
	        nativeQuery = true)
	    List<ProjectTeamView> findWorkspaceMembersWithProject(@Param("workspaceId") Long workspaceId);

	@Modifying
	@Query(value =
	    "MERGE INTO project_members pm " +
	    "USING dual " +
	    "ON (pm.project_id = :projectId " +
	    "AND pm.workspace_member_id = :workspaceMemberId) " +

		"WHEN MATCHED THEN " +
		"UPDATE SET " +
		"    pm.role = :role, " +
		"    pm.status = 0, " +
		"    pm.LEFT = NULL, " +
		"    pm.updated = SYSDATE " +

	    "WHEN NOT MATCHED THEN " +
	    "INSERT (project_member_id, project_id, workspace_member_id, " +
	    "role, status, joined) " +
	    "VALUES (seq_project_members.NEXTVAL, :projectId, " +
	    ":workspaceMemberId, :role, 0, SYSDATE)",
	    nativeQuery = true)
	void upsertProjectMember(
	    @Param("workspaceMemberId") Long workspaceMemberId,
	    @Param("projectId") Long projectId,
	    @Param("role") int role
	);
	
	@Query(value = "SELECT role FROM workspace_members WHERE workspace_id = :workspaceId AND member_id = :memberId", nativeQuery = true)
    Integer findRoleByWorkspaceIdAndMemberId(@Param("workspaceId") Long workspaceId, @Param("memberId") Long memberId);
	
	@Modifying
    @Query(value = "UPDATE project_members SET status = 1, left = SYSDATE WHERE project_member_id = :projectMemberId", nativeQuery = true)
    void excludeFromProject(@Param("projectMemberId") Long projectMemberId);

    @Modifying
    @Query(value = "UPDATE workspace_members SET status = 1, left = SYSDATE WHERE workspace_member_id = :workspaceMemberId", nativeQuery = true)
    void excludeFromWorkspace(@Param("workspaceMemberId") Long workspaceMemberId);
	
}
