package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MypageDto {
	
	private Long memberId;

    private String loginId;
    private String name;
    private String email;
    private String birth;

    private String phone;
    private String address;

    private String profile;
}
