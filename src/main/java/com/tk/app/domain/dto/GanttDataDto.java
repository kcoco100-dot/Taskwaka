package com.tk.app.domain.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GanttDataDto {
	private LocalDate ganttStart; // 시작일
	private LocalDate ganttEnd; // 종료일
	private List<LocalDate> ganttDate; // 간트 목록
}
