package com.tk.app.domain.dto;

import com.tk.app.common.MyUtil;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManageNoticeDto {
    private Long notice_id;
    private Long workspace_id;
    private Long project_id;
    private Long task_id;
    private Long member_id;

    private String notice_type;
    private String title;
    private String content;
    private String created;
    private String updated;
    private String deleted;

    private int status;
    private int view_count;

    private Long notice_file_id;
    private Long file_size;
    private String original_name;
    private String stored_name;
    private String file_path;
    private String uploaded;

    private int file_count;
    private String member_name;
    private String project_name;
    private String task_name;

    private String domainKey;
    private Long requester_member_id;
    public void setContent(String content) {
        this.content = MyUtil.sanitize(content);
    }
}
