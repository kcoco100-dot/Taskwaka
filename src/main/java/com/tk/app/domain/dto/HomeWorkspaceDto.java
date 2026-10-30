package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HomeWorkspaceDto {
    private long workspaceId; // 워크번호
    private long memberId;    // 회원번호
    private String subject;  // 워크명
    private int status;      // 공개상태: 0=공개, 1=비공개
    private String domainKey;

    private String active;   // 활성상태: Y=활성, N=비활성
    private String reason;   // 변경사유: 비활성일 때 정지 사유로 표시
    private String content;   // 워크소개
}
