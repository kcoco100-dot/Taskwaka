package com.tk.app.domain.dto;

import lombok.Data;

@Data
public class TaskMemberDto {
    private Long taskId;
    private Long subTaskId;
    private Long projectMemberId;
    private Long memberId;
    private String memberName;
    private Integer role;
    private String profile;
    private String created;
}