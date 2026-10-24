package com.tk.app.domain.dto;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class ManageFileDto {
    private Long fileId;
    private Long reportId;
    private String originalName;
    private String storedName;
    private String path;
    private Long fileSize;
}
