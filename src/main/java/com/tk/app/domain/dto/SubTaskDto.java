package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SubTaskDto {
    private Long subTaskId;
    private Long taskId;
    private Long projectId;
    private Long projectMemberId;
    private String memberName;
    private String title;
    private String content;
    private String startAt;
    private String endAt;
    private Integer status;
    private Double progress;
    private String created;
    private String updated;
}
