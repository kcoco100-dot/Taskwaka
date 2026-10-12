package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class TaskDto {
    private Long taskId;
    private Long projectId;
    private Long projectMemberId;
    private String memberName;      // 조인해서 가져올 담당자/작성자 이름
    private String title;
    private String content;
    private Integer taskLevel;      // 그룹 번호 (1: 그룹1, 0: 그룹 미지정 등)
    private String startAt;         // YYYY-MM-DD
    private String endAt;           // YYYY-MM-DD (DB의 "END" 컬럼)
    private Integer status;         // 0: 요청, 1: 진행, 2: 피드백, 3: 완료, 4: 보류
    private Double progress;
    private String created;
    private String updated;

    // 1:N 관계의 하위업무 목록
    private List<SubTaskDto> subTasks;
}
