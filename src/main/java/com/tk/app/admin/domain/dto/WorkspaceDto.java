package com.tk.app.admin.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceDto {
    private long workspaceId; // 워크번호
    private long memberId; // 회원번호
    private String subject; // 워크명
    private int status; // 워크 공개상태 0=공개, 1=비공개
    
    private String domainKey; // 워크 HOME 으로 이동 시 사용하는 주소 키
}