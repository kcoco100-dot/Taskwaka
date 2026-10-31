package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrganizationModalDto {

    private String domainKey;
    private Long workId;
    private Long memberId;
    private Long projectId;

    private String subject;
    private String startAt;
    private String endAt;

    private Long projectMemberId;
    private String name;
    private Integer role;
    private String taskTitles;
}
