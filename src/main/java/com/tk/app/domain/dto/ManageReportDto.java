package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageReportDto {
	private Long reportId;
    private Long reviewId;
    private Long projectId;
    private Long taskId;
    private Long projectMemberId;

    private int reportCategory;
    private String reportTypeName;

    private String projectName;
    private String taskName;
    private String reportTitle;
    private String content;
    private String authorName;
    private String createdDate;

    private String stageName;

    private Double progress;
    private int reviewStatus;

    private String reviewComment;
    private String rejectReason;
    private String reviewedDate;

    private int canReview;
}
