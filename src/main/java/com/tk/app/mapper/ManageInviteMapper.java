package com.tk.app.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.ManageInviteDto;

@Mapper
public interface ManageInviteMapper {
    
    public Long selectWorkspaceIdByDomainKey(String domainKey);
	public List<ManageInviteDto> selectEligibleMembers(Map<String, Object> map);
	public ManageInviteDto selectMemberInfoForInvite(Long memberId);
	public void insertManageInvite(ManageInviteDto dto);
	public ManageInviteDto selectManageInviteByKey(String inviteKey);
	public int updateManageInviteAccept(ManageInviteDto dto);
	public void insertManageInviteMember(ManageInviteDto dto);
	public List<ManageInviteDto> selectSearchEligibleMembers(Map<String, Object> map);
	
	public String selectInviteDomainByWorkspaceId(Long workspaceId);
	public Long lockActiveInviteMember(Long memberId);
	public ManageInviteDto selectInviteWorkspaceMember(Map<String, Object> map);
}
