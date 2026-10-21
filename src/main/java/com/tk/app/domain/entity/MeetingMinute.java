package com.tk.app.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "meeting_minutes")
public class MeetingMinute {
	@Id
	@Column(name = "meeting_minute_id", nullable = false, updatable = false, columnDefinition = "NUMBER")
	private Long meetingMinuteId;
	
	@Column(name = "sub_task_id", nullable = false, updatable = false, columnDefinition = "NUMBER")
	private Long subTaskId;
	
	@Column(name = "member_id", nullable = false, updatable = false, columnDefinition = "NUMBER")
	private Long memberId;
	
	@Column(name = "project_id", nullable = false, updatable = false, columnDefinition = "NUMBER")
	private Long projectId;
	
	@Column(name = "title", nullable = false, length = 200)
	private String title;
	
	@Lob // CLOB 매핑하도록 지정함
	@Column(name = "content", nullable = false)
	private String content;
	
	@Column(name = "meeting")
	private LocalDateTime meeting;
	
	@Column(name = "created", nullable = false)
	private LocalDateTime created;
	
	@Column(name = "updated")
	private LocalDateTime updated;
	
	@Column(name = "deleted")
	private LocalDateTime deleted;
}
