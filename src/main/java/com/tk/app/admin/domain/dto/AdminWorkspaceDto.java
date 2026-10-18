
package com.tk.app.admin.domain.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminWorkspaceDto {

    private long workspace_id;
    private String subject;
    private String domain_key;
    private String pm_name;
    private int member_count;
    private String active;
    private String reason;
    private Date status;

}
