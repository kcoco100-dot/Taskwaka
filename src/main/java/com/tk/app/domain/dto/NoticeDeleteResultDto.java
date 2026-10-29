package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeDeleteResultDto {
    private int deletedCount; // 삭제된 공지 건수
    private long revokedExp; // 실제 차감된 경험치
}
