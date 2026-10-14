package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageReportDto {
	private Long reviewId;

    private Long projectReportId;
    private Long teamReportId;
    private Long dailyReportId;

    private Long projectId;
    private Long taskId;
    private Long subTaskId;
    private Long projectMemberId;

    private Integer reportCategory;

    private String projectName;
    private String taskName;
    private String reportTitle;
    private String content;
    private String authorName;
    private String createdDate;
    private Double progress;

    private Integer reviewStatus;
    private String reviewComment;
    private String rejectReason;
    private String reviewedDate;

    private Integer role;
    private String reportTypeName;
}
