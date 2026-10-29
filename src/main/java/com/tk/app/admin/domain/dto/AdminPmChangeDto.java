
package com.tk.app.admin.domain.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPmChangeDto {

    private long pm_change_id;
    private long workspace_id;
    private long old_member_id;
    private long new_member_id;

    private String reason;
    private int status;
    private Date requested;
    private Date processed;
    private String reject_reason;
    private Long admin_id;
    
    private String workspace_name;
    private String old_pm_name;
    private String new_pm_name;
    
}
