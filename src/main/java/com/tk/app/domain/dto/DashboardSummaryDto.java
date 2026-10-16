package com.tk.app.domain.dto;

import lombok.Data;

@Data
public class DashboardSummaryDto {
    private int totalCount;      // 전체 업무 수
    private int completedCount;  // 완료된 업무 수 (status == 3)
    private int inProgressCount; // 진행 중인 업무 수 (status == 1 or 2)
    private int pendingCount;    // 요청/대기 업무 수 (status == 0)
    private double completionRate; // 전체 완료율 (%)
}