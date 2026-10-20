package com.tk.app.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkChatMemberDto {

	private Long memberId;
	private String name;
	private Integer role;
	private String roleName;
	private String profilePhoto;
	private Long workId;
	private String domainKey;
	
	private Long workMemberId;
	private Long loginMemberId;
	private Long chatRoomId;
	private Long myWorkId;
	private Long targetWorkId;
	private Long targetMemberId;
	private String targetName;
}
