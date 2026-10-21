package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManageWorkDto {
    private Long workspaceMemberId;
    private Long memberId;
    private String name;
    private String email;
    private int role;
    
}