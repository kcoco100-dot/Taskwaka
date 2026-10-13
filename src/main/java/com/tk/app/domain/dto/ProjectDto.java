package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ProjectDto {
    private Long projectId;        // 프로젝트 번호
    private Long workspaceId;      // 워크스페이스 번호
    private String subject;        // 프로젝트명
    private String content;        // 내용 (CLOB)
    private String startAt;        // 시작일 (YYYY-MM-DD)
    private String endAt;          // 종료일 (YYYY-MM-DD, DB 컬럼: "END")
    private Integer status;        // 진행상태 (0: 예정, 1: 진행중, 2: 완료)
    private Double progress;       // 진행률 (0 ~ 100)
    private String archive;        // 보관여부 (Y / N)
    private String created;        // 생성일시 (YYYY-MM-DD HH24:MI:SS)
    private String updated;        // 수정일시 (YYYY-MM-DD HH24:MI:SS)
    private String deleted;        // 삭제일시 (YYYY-MM-DD HH24:MI:SS)
}
