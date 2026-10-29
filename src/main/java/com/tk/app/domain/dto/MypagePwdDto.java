package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MypagePwdDto {

    private Long memberId;

    private String currentPwd;
    private String newPwd;
    private String confirmPwd;

    private String pwd;
}
