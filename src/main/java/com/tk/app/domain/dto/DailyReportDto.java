package com.tk.app.domain.dto;

import lombok.Data;

@Data
public class DailyReportDto {
    private Long dailyReportId;
    private Long subTaskId;
    private Long projectMemberId;
    private String writerName;
    private String reportDate;
    private String content;
    private Double progress;
    private Integer status;        // 0=제출(확인전), 1=승인(확인완료), 2=반려
    private String created;
    private String updated;

    private Integer reviewStatus;
    private String viewedAt;
    private boolean modifiable;    // 상급자 확인 전 여부 (true일 때만 수정/삭제 가능)
}