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
        "   wm.role AS workspaceRole, " +
        "   wm.status AS status, " +
        "   pm.project_member_id AS projectMemberId, " +
        "   p.project_id AS projectId, " +
        "   p.subject AS projectName, " +
        "   pm.role AS role " +
        "FROM workspace_members wm " +
        "JOIN member_details md ON wm.member_id = md.member_id " +
        "LEFT JOIN project_members pm ON wm.workspace_member_id = pm.workspace_member_id AND pm.status = 0 " +
        "AND EXISTS (SELECT 1 FROM projects active_p WHERE active_p.project_id = pm.project_id " +
        "AND active_p.workspace_id = wm.workspace_id AND active_p.deleted IS NULL) " +
        "LEFT JOIN projects p ON pm.project_id = p.project_id AND p.workspace_id = wm.workspace_id AND p.deleted IS NULL " +
        "WHERE wm.workspace_id = :workspaceId AND wm.status = 0 " +
        "AND (:kwd IS NULL OR INSTR(LOWER(md.name), LOWER(:kwd)) > 0) " +
        "ORDER BY md.name ASC, wm.workspace_member_id, p.project_id DESC", 
        nativeQuery = true)
	    List<ProjectTeamView> findWorkspaceMembersWithProject(@Param("workspaceId") Long workspaceId, @Param("kwd") String kwd);

    // 프로젝트 팀원 등록 및 직위 변경
    @Modifying
    @Query(value =
        "MERGE INTO project_members pm " +
        "USING dual " +
        "ON (pm.project_id = :projectId " +
        "AND pm.workspace_member_id = :workspaceMemberId) " +
        "WHEN MATCHED THEN " +
        "UPDATE SET pm.role = :role, " +
        "pm.status = 0, " +
        "pm.\"LEFT\" = NULL, " +
        "pm.updated = SYSDATE " +
        "WHEN NOT MATCHED THEN " +
        "INSERT (project_member_id, project_id, workspace_member_id, " +
        "role, status, joined) " +
        "VALUES (seq_project_members.NEXTVAL, :projectId, " +
        ":workspaceMemberId, :role, 0, SYSDATE)",
        nativeQuery = true)
    void upsertProjectMember(
        @Param("workspaceMemberId") Long workspaceMemberId, @Param("projectId") Long projectId, @Param("role") int role
    );
	
    // 활성 워크스페이스 직위 조회
    @Query(value =
        "SELECT role FROM workspace_members " +
        "WHERE workspace_id = :workspaceId " +
        "AND member_id = :memberId " +
        "AND status = 0",
        nativeQuery = true)
    Integer findRoleByWorkspaceIdAndMemberId(@Param("workspaceId") Long workspaceId, @Param("memberId") Long memberId
    );
    
    // 프로젝트 참여자 제외
    @Modifying
    @Query(value =
        "UPDATE project_members " +
        "SET status = 1, \"LEFT\" = SYSDATE, updated = SYSDATE " +
        "WHERE project_member_id = :projectMemberId AND status = 0",
        nativeQuery = true)
    int excludeFromProject(@Param("projectMemberId") Long projectMemberId
    );

    // 워크스페이스 참여자 제외
    @Modifying
    @Query(value =
        "UPDATE workspace_members " +
        "SET status = 1, \"LEFT\" = SYSDATE " +
        "WHERE workspace_member_id = :workspaceMemberId AND status = 0",
        nativeQuery = true)
    int excludeFromWorkspace(@Param("workspaceMemberId") Long workspaceMemberId
    );
    

    // 프로젝트와 워크스페이스 팀원 일치 확인
    @Query(value =
        "SELECT COUNT(*) FROM projects p " +
        "JOIN workspace_members wm " +
        "ON p.workspace_id = wm.workspace_id " +
        "WHERE p.project_id = :projectId " +
        "AND p.workspace_id = :workspaceId " +
        "AND p.deleted IS NULL " +
        "AND wm.workspace_member_id = :workspaceMemberId " +
        "AND wm.status = 0",
        nativeQuery = true)
    int countValidAssignmentTarget(@Param("workspaceId") Long workspaceId, @Param("workspaceMemberId") Long workspaceMemberId, @Param("projectId") Long projectId
    );

    // 기존 프로젝트 참여자 번호 조회
    @Query(
        "SELECT pm.projectMemberId " +
        "FROM ManageProjectMember pm " +
        "WHERE pm.projectId = :projectId " +
        "AND pm.workspaceMemberId = :workspaceMemberId"
    )
    Long findAssignedProjectMemberId(@Param("projectId") Long projectId, @Param("workspaceMemberId") Long workspaceMemberId
    );

    // 프로젝트 참여자의 직위 조회
    @Query(
        "SELECT pm.role FROM ManageProjectMember pm " +
        "WHERE pm.projectMemberId = :projectMemberId"
    )
    Integer findAssignedProjectRole(@Param("projectMemberId") Long projectMemberId
    );

    // 해당 워크스페이스의 활성 프로젝트 참여자인지 확인
    @Query(value =
        "SELECT COUNT(*) FROM project_members pm " +
        "JOIN projects p ON pm.project_id = p.project_id " +
        "WHERE pm.project_member_id = :projectMemberId " +
        "AND p.workspace_id = :workspaceId " +
        "AND p.deleted IS NULL " +
        "AND pm.status = 0",
        nativeQuery = true)
    int countActiveProjectMember(@Param("workspaceId") Long workspaceId, @Param("projectMemberId") Long projectMemberId
    );

    // 워크스페이스 참여자 확인
    @Query(value =
        "SELECT COUNT(*) FROM workspace_members " +
        "WHERE workspace_member_id = :workspaceMemberId " +
        "AND workspace_id = :workspaceId " +
        "AND status = 0",
        nativeQuery = true)
    int countActiveWorkspaceMember(@Param("workspaceId") Long workspaceId, @Param("workspaceMemberId") Long workspaceMemberId
    );

    // 워크스페이스 팀원이 참여한 프로젝트 번호 조회
    @Query(value =
        "SELECT pm.project_member_id " +
        "FROM project_members pm " +
        "JOIN projects p ON pm.project_id = p.project_id " +
        "WHERE pm.workspace_member_id = :workspaceMemberId " +
        "AND p.workspace_id = :workspaceId " +
        "AND pm.status = 0",
        nativeQuery = true)
    List<Long> findActiveProjectMemberIds(@Param("workspaceId") Long workspaceId, @Param("workspaceMemberId") Long workspaceMemberId
    );

    // 하위업무 연결이 없는 TL 업무 참여 관계 삭제
    @Modifying
    @Query(value =
        "DELETE FROM task_members tm " +
        "WHERE tm.project_member_id = :projectMemberId " +
        "AND NOT EXISTS ( " +
        "SELECT 1 FROM sub_task_members stm " +
        "WHERE stm.task_id = tm.task_id " +
        "AND stm.project_member_id = tm.project_member_id)",
        nativeQuery = true)
    int deleteUnlinkedTaskMembers(@Param("projectMemberId") Long projectMemberId
    );
    

    @Query(value = """
	    SELECT COUNT(*)
	    FROM tasks t
	    JOIN projects p ON p.project_id = t.project_id
	    JOIN project_members pm ON pm.project_member_id = t.project_member_id
	     AND pm.project_id = t.project_id
	    JOIN workspace_members wm ON wm.workspace_member_id = pm.workspace_member_id
	     AND wm.workspace_id = p.workspace_id
	    WHERE t.project_member_id = :projectMemberId
	      AND p.workspace_id = :workspaceId
	      AND p.deleted IS NULL
	      AND t.deleted IS NULL
	      AND pm.status = 0
	      AND pm."LEFT" IS NULL
	      AND wm.status = 0
	      AND wm."LEFT" IS NULL
	      AND EXISTS (
	          SELECT 1
	          FROM task_members tm
	          WHERE tm.task_id = t.task_id
	            AND tm.project_member_id = pm.project_member_id
	      )
	    """, nativeQuery = true)
	int countAssignedTasks(
	        @Param("workspaceId") Long workspaceId,
	        @Param("projectMemberId") Long projectMemberId);
    
    @Query(value = """
        SELECT COUNT(*) FROM tasks t
        JOIN projects p ON p.project_id = t.project_id
        JOIN project_members pm ON pm.project_member_id = t.project_member_id AND pm.project_id = p.project_id
        WHERE p.workspace_id = :workspaceId AND pm.workspace_member_id = :workspaceMemberId
          AND p.deleted IS NULL AND t.deleted IS NULL
        """, nativeQuery = true)
    int countWorkspaceAssignedTasks(@Param("workspaceId") Long workspaceId, @Param("workspaceMemberId") Long workspaceMemberId);
    
    // 프로젝트 팀원 관리
    @Query(value = """
    	SELECT  workspace_id
	    FROM workspace_domains
	    WHERE domain_key = :domainKey
	      AND active = 'Y'
	    """, nativeQuery = true)
	Long findWorkspaceIdForTeam(@Param("domainKey") String domainKey);
    
    // 현재 로그인 회원이 관리할 수 있는 프로젝트 목록
    @Query(value = """
        SELECT p.project_id, p.subject
        FROM projects p
        WHERE p.workspace_id = :workspaceId
          AND p.deleted IS NULL
          AND EXISTS (
              SELECT 1
              FROM workspace_members actor
              WHERE actor.workspace_id = p.workspace_id
                AND actor.member_id = :actorId
                AND actor.status = 0
                AND actor."LEFT" IS NULL
                AND (
                    actor.role = 70
                    OR (
                        actor.role = 50
                        AND EXISTS (
                            SELECT 1
                            FROM project_members mine
                            WHERE mine.project_id = p.project_id
                              AND mine.workspace_member_id = actor.workspace_member_id
                              AND mine.status = 0
                              AND mine."LEFT" IS NULL
                        )
                    )
                )
          )
        ORDER BY p.project_id DESC
        """, nativeQuery = true)
    List<Object[]> findProjectsForTeamManagement(@Param("workspaceId") Long workspaceId, @Param("actorId") Long actorId
    );
    
    // 제외 대상자의 하위업무 참여 관계 삭제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        DELETE FROM sub_task_members
        WHERE project_member_id = :projectMemberId
        """, nativeQuery = true)
    int deleteSubTaskMembersForExclusion(@Param("projectMemberId") Long projectMemberId
    	);


    // 워크 제외 시 기존 비활성 프로젝트 참여 이력까지 조회
    @Query(value = """
        SELECT pm.project_member_id
        FROM project_members pm
        JOIN projects p
          ON p.project_id = pm.project_id
        WHERE pm.workspace_member_id = :workspaceMemberId
          AND p.workspace_id = :workspaceId
        ORDER BY pm.project_member_id
        """, nativeQuery = true)
    List<Long> findProjectMemberIdsForWorkspaceExclusion(@Param("workspaceId") Long workspaceId, @Param("workspaceMemberId") Long workspaceMemberId
    	);
    
}
