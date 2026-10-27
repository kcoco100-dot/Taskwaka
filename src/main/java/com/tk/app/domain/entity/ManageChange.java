package com.tk.app.domain.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="pm_change")
@Getter
@Setter
@NoArgsConstructor
public class ManageChange {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pm_change_gen")
	@SequenceGenerator(name = "seq_pm_change_gen", sequenceName = "seq_pm_change", allocationSize = 1)
	@Column(name = "pm_change_id")
	private long pm_change_id;
	
	@Column(name = "workspace_id", nullable = false)
    private long workspace_id;
    
	@Column(name = "old_member_id", nullable = false)
    private long old_member_id;
    
	@Column(name = "new_member_id", nullable = false)
    private long new_member_id;
    
	@Column(name = "reason", nullable = false, length = 1000)
    private String reason;
    
	@Column(name = "status", nullable = false)
    private int status = 0;
    
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "requested", nullable = false)
    private Date requested;
    
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "processed")
    private Date processed;
    
	@Column(name = "reject_reason", length = 1000)
    private String reject_reason;
    
	@Column(name = "admin_id")
    private Long admin_id;
    
	@Transient
    private String workspace_name;
    
	@Transient
    private String old_pm_name;
    
	@Transient
    private String new_pm_name;
    
}
