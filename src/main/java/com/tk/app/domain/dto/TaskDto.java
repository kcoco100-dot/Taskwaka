package com.tk.app.domain.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class TaskDto {
    private Long taskId;
    private Long projectId;
    private Long projectMemberId; // 지명된 메인 업무 담당자 번호
    private Long assigneeMemberId; // 지명된 메인 업무 담당자 멤버 아이디
    private String assigneeName;  // 담당자 실명 (member_details.name 조인)
    private String title;
    private String content;
    private Integer taskLevel;
    private String startAt;
    private String endAt;
    private Integer status;
    private Double progress;
    private String created;
    private String updated;
    
    private String saveFilename;
	private String originalFilename;
	private List<MultipartFile> selectFile; // <input type='file' name='selectFile' ..
	
    private List<SubTaskDto> subTasks;
    private List<TaskMemberDto> participants;
}