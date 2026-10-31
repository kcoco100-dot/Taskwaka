package com.tk.app.domain.dto;

import java.util.Map;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class SubTaskDto {
    private Long subTaskId;
    private Long taskId;
    private Long projectId;
    private Long projectMemberId; // 지명된 하위업무 담당자 번호
    private Long assigneeMemberId; // 지명된 하위업무 담당자 번호
    private String assigneeName;  // 하위업무 담당자 실명
    private String title;
    private String content;
    private String startAt;
    private String endAt;
    private Integer status;
    private Double progress;
    private String created;
    private String updated;
    
    private String saveFilename;
	private String originalFilename;
	private List<Map<String,Object>> files;
	private List<MultipartFile> selectFile; // <input type='file' name='selectFile' ..

    private List<TaskMemberDto> participants;
}