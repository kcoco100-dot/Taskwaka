package com.tk.app.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "project_members")
@Getter
@Setter
@NoArgsConstructor
public class ManageProjectMember {

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_project_members_gen")
    @SequenceGenerator(name = "seq_project_members_gen", sequenceName = "seq_project_members", allocationSize = 1)
    @Column(name = "project_member_id")
	private Long projectMemberId;
	
	@Column(name = "project_id", nullable = false)
    private Long projectId;
	
	@Column(name = "workspace_member_id", nullable = false)
    private Long workspaceMemberId;
	
	@Column(name = "role", nullable = false)
    private Integer role;
	
	@Column(name = "status", nullable = false)
    private Integer status = 0;
	
	@Column(name = "joined", nullable = false)
    private LocalDateTime joined;
	
	@Column(name = "\"left\"")
	private LocalDateTime left;
	
}
