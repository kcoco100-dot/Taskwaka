package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTeamDto {
    private Long projectMemberId;
    private Long workspaceMemberId;
    private String name;
    private String email;
    private Integer role;
    private Integer status;
}
