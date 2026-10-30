package com.tk.app.domain.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeTodayScheduleDto {

    private String scheduleType; // TASK = 업무, PROJECT = 프로젝트
    private String title;        // 업무명 또는 프로젝트명
    private String description;  // 업무의 프로젝트명 또는 프로젝트 기간
    private LocalDate endAt;     // 마감일 정렬에 사용
    private Integer status;      // 업무 상태: 0=할일, 1=진행중, 2=검토중, 3=완료
}
