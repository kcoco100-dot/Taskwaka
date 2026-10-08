package com.tk.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.MemberDto;

@Mapper
public interface MemberMapper {
	public MemberDto loginSnsMember(Map<String, Object> map);

	public Long memberSeq();	
	public void insertMember1(MemberDto dto) throws SQLException;
	public void insertMember2(MemberDto dto) throws SQLException;
	public void insertMember12(MemberDto dto) throws SQLException;
	public void insertSnsMember(MemberDto dto) throws SQLException;
	public void insertMemberStatus(MemberDto dto) throws SQLException;
	
	public void updateMemberEnabled(Map<String, Object> map) throws SQLException;
	public void updateMemberPassword(MemberDto dto) throws SQLException;
	
	public void updateMember1(MemberDto dto) throws SQLException;
	public void updateMember2(MemberDto dto) throws SQLException;
	public void deleteProfilePhoto(Map<String, Object> map) throws SQLException;

	public void updateLastLogin(Long member_id) throws SQLException;
	public void updateLastLoginId(String login_id) throws SQLException;
	
	public MemberDto findById(Long member_id);
	public MemberDto findByLoginId(String login_id);
	public Long getMemberId(String login_id);
	
	public int checkFailureCount(String login_id);
	public void updateFailureCountReset(String login_id) throws SQLException;
	public void updateFailureCount(String login_id) throws SQLException;
	
	public void deleteMember1(Map<String, Object> map) throws SQLException;
	public void deleteMember2(Map<String, Object> map) throws SQLException;
	
	public List<MemberDto> listFindMember(Map<String, Object> map);
	
	public void insertAuthority(MemberDto dto) throws SQLException;
	public void deleteAuthority(Map<String, Object> map) throws SQLException;
	public String findByAuthority(String login_id);
}
