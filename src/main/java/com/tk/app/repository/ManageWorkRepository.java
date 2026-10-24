package com.tk.app.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tk.app.domain.dto.ManageWorkDto;
import com.tk.app.domain.entity.ManageWorkspaceMember;

public interface ManageWorkRepository extends JpaRepository<ManageWorkspaceMember, Long>{
	
	// 워크 직위 조회
	@Query("""
			SELECT MAX(wm.role)
			FROM ManageWorkspaceMember wm
			WHERE wm.workspaceId = :workspaceId AND wm.memberId = :memberId AND wm.status = 0
			""")
	int findWorkRole(
			@Param("workspaceId") Long workspaceId,
			@Param("memberId") Long memberId
		);
	
	// 워크 팀원 수
	@Query("""
			SELECT COUNT(wm)
			FROM ManageWorkspaceMember wm
			LEFT JOIN ManageMemberDetail md ON md.memberId = wm.memberId
			WHERE wm.workspaceId = :workspaceId AND wm.status = 0
			  AND (LOWER(COALESCE(md.name, ' ')) LIKE :kwd OR LOWER(COALESCE(md.email, ' ')) LIKE :kwd)
			""")
	long dataCount(
	    @Param("workspaceId") Long workspaceId,
	    @Param("kwd") String kwd
	);

	 // 워크 팀원 목록
    @Query("""
        SELECT new com.tk.app.domain.dto.ManageWorkDto(
            wm.workspaceMemberId, wm.memberId, md.name, md.email, wm.role
        )
        FROM ManageWorkspaceMember wm
        LEFT JOIN ManageMemberDetail md ON md.memberId = wm.memberId
        WHERE wm.workspaceId = :workspaceId AND wm.status = 0 AND (LOWER(COALESCE(md.name, ' ')) LIKE :kwd OR LOWER(COALESCE(md.email, ' ')) LIKE :kwd)
        ORDER BY CASE WHEN wm.role = 70 THEN 0 ELSE 1 END, md.name ASC, wm.workspaceMemberId ASC
        """)
    List<ManageWorkDto> listWorkMember(
        @Param("workspaceId") Long workspaceId,
        @Param("kwd") String kwd,
        Pageable pageable
    );

    // 워크스페이스 직위 변경
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE ManageWorkspaceMember wm
        SET wm.role = :role
        WHERE wm.workspaceMemberId = :workspaceMemberId
          AND wm.workspaceId = :workspaceId
          AND wm.status = 0
          AND EXISTS (
              SELECT actor.workspaceMemberId
              FROM ManageWorkspaceMember actor
              WHERE actor.workspaceId = wm.workspaceId
                AND actor.memberId = :actorId
                AND actor.status = 0
          )
        """)
    int updateWorkRole(
        @Param("workspaceId") Long workspaceId,
        @Param("actorId") Long actorId,
        @Param("workspaceMemberId") Long workspaceMemberId,
        @Param("role") int role
    );
	
	
}
