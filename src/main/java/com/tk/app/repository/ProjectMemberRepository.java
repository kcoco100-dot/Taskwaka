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

	@Modifying
    @Query(value = 
        "MERGE INTO project_members pm " +
        "USING dual " +
        "ON (pm.project_id = :projectId AND pm.workspace_member_id = :workspaceMemberId) " +
        "WHEN MATCHED THEN " +
        "    UPDATE SET pm.role = :role, pm.updated = SYSDATE " +
        "WHEN NOT MATCHED THEN " +
        "    INSERT (project_member_id, project_id, workspace_member_id, role, status, joined) " +
        "    VALUES (seq_project_members.NEXTVAL, :projectId, :workspaceMemberId, :role, 0, SYSDATE)", 
        nativeQuery = true)
    void upsertProjectMember(@Param("workspaceMemberId") Long workspaceMemberId, 
                             @Param("projectId") Long projectId, 
                             @Param("role") int role);
	
	@Query(value = "SELECT role FROM workspace_members WHERE workspace_id = :workspaceId AND member_id = :memberId", nativeQuery = true)
    Integer findRoleByWorkspaceIdAndMemberId(@Param("workspaceId") Long workspaceId, @Param("memberId") Long memberId);
	
}
