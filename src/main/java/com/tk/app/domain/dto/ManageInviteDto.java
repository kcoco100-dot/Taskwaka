package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageInviteDto {
    private String inviteKey;
    private Long workspaceId;
    private Long inviterId;
    private Long acceptedMemberId;
    private String email;
    private String name;
    
    
    private Integer status;
    
    private String expires;
    private String accepted;
    private String created;
    private Integer inviteStatus;
    

    private Long workspaceMemberId;
    private Long memberId;
    private String joined;
    private String left;
    private Integer role;
    
}
