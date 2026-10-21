package com.tk.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tk.app.domain.entity.MeetingMinute;

public interface MeetingMinuteRepository extends JpaRepository<MeetingMinute, Long> {
	
	@Modifying
	@Query(value = """
			UPDATE meeting_minutes mm
			SET title = :title,
				content = :content,
				updated = SYSDATE
			WHERE mm.meeting_minute_id = :meetingMinuteId
				AND mm.deleted IS NULL
				AND EXISTS (
					SELECT 1
					FROM projects p
					JOIN project_members pm
						ON pm.project_id = p.project_id
					JOIN workspace_members wm
						ON wm.workspace_member_id = pm.workspace_member_id
						AND wm.workspace_id = p.workspace_id
					WHERE p.project_id = mm.project_id
						AND p.workspace_id = :workspaceId
						AND wm.member_id = :memberId
						AND wm.status = 0
						AND pm.left IS NULL
						AND (
							mm.member_id = :memberId
							OR pm.role IN (70, 50, 30)
						)
			)
	""", nativeQuery = (true))
	int meetingsEdit(
			@Param("workspaceId") Long workspaceId,
			@Param("meetingMinuteId") Long meetingMinuteId,
			@Param("memberId") Long memberId,
			@Param("title") String title,
			@Param("content") String content);
	
	@Modifying
	@Query(value = """
			UPDATE meeting_minutes mm
			SET updated = SYSDATE,
			    deleted = SYSDATE
			WHERE mm.meeting_minute_id = :meetingMinuteId
			    AND mm.deleted IS NULL
			    AND EXISTS (
			        SELECT 1
			        FROM projects p
			        JOIN project_members pm 
			            ON pm.project_id = p.project_id
			        JOIN workspace_members wm 
			            ON wm.workspace_member_id = pm.workspace_member_id
			            AND wm.workspace_id = p.workspace_id
			        WHERE p.project_id = mm.project_id
				        AND p.workspace_id = :workspaceId
				        AND wm.member_id = :memberId
				        AND wm.status = 0
				        AND pm.left IS NULL
				        AND (
				            mm.member_id = :memberId
				            OR pm.role IN (70, 50, 30)
				        )
			    )
			""", nativeQuery = (true))
	int meetingsDelete(
			@Param("workspaceId") Long workspaceId,
			@Param("meetingMinuteId") Long meetingMinuteId,
			@Param("memberId") Long memberId);
	
	// 회의록 게시글 조회
	@Query(value = """
			SELECT COUNT(*)
			FROM meeting_minutes m
			JOIN projects p ON p.project_id = m.project_id
			WHERE p.workspace_id = :workspaceId
			  AND m.deleted IS NULL
			  AND EXISTS (
			      SELECT 1
			      FROM project_members pm
			      JOIN workspace_members wm
			        ON wm.workspace_member_id = pm.workspace_member_id
			       AND wm.workspace_id = p.workspace_id
			      WHERE pm.project_id = p.project_id
			        AND wm.member_id = :memberId
			        AND wm.status = 0
			        AND pm.left IS NULL
			  )
				AND (:kwd IS NULL OR m.title LIKE '%' || :kwd || '%')
			""", nativeQuery = (true))
	int countMeetingMinutes(
			@Param("workspaceId") Long workspaceId,
			@Param("memberId") Long memberId,
			@Param("kwd") String kwd);
}
