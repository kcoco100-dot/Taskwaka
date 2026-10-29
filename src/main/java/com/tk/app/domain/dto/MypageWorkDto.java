package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MypageWorkDto {
	
	private Long memberId;

    private Long workId;
    private Long workMemberId;

    private String subject;
    private String domainKey;

    private Integer role;
}
