package com.tk.app.domain.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WorkspaceMemberGroupDto {
	private Long workspaceMemberId;
    private String name;
    private Integer status;
    
    private List<ProjectParticipation> projects = new ArrayList<>();

    @Data
    public static class ProjectParticipation {
        private Long projectMemberId;
        private Long projectId;
        private String projectName;
        private Integer role;
    }
}
