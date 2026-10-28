package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MypageDto {

    private String loginId;
    private String name;
    private String email;
    private String birth;

    private String phone;
    private String address;

    private String profile;
}
