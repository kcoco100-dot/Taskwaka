package com.tk.app.domain.dto;

import lombok.Data;

@Data
public class CalendarEventDto {
    private Long scheduleId;
    private String scheduleType; // PERSONAL(개인일정), TASK(메인업무), SUB_TASK(하위업무)
    private Long workspaceId;
    private Long workspaceMemberId;
    private Long projectId;
    private Long taskId;
    private Long subTaskId;
    private String title;
    private String content;
    private String startAt;      // YYYY-MM-DD
    private String endAt;        // YYYY-MM-DD
    private Integer status;
    private Double progress;
    private String assigneeName;
    private String createdAt;
}