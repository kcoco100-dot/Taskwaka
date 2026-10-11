package com.tk.app.domain.entity;


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
@Table(name = "workspace_members")
@Getter @Setter
@NoArgsConstructor
public class ManageWorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_workspace_members_gen")
    @SequenceGenerator(name = "seq_workspace_members_gen", sequenceName = "seq_workspace_members", allocationSize = 1)
    @Column(name = "workspace_member_id")
    private Long workspaceMemberId;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "status", nullable = false)
    private Integer status = 0;
}
