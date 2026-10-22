package com.tk.app.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageNoticeDto {
	private Long notice_id;
    private Integer notice_type;
    private Long workspace_id;
    private Long project_id;
    private Long task_id;
    private Long member_id;
    private Long requester_member_id;
    private String domainKey;

    private String title;
    private String content;
    private String created;
    private String updated;
    private String deleted;

    private Integer status;
    private Integer view_count;

    private Long notice_file_id;
    private String original_name;
    private String stored_name;
    private String file_path;
    private Long file_size;
    private String uploaded;

    private Integer file_count;
    private String member_name;
	
}
