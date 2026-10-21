package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeCreateResultDto {
    private int insertCount;
    private GrowthLevelDto levelUpInfo;

}
