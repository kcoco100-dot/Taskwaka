package com.tk.app.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {
	private final MemberMapper mapper;
	
	@Value("${file.upload-root}/member")
	private String uploadPath;
	
	@Override
	public MemberDto loginSnsMember(Map<String, Object> map) {

		return null;
	}

	@Override
	public void insertMember(MemberDto dto) throws Exception {

	}

	@Override
	public void insertSnsMember(MemberDto dto) throws Exception {

	}

	@Override
	public void insertMemberStatus(MemberDto dto) throws Exception {
		try {
			mapper.insertMemberStatus(dto);
		} catch (Exception e) {
			log.info("insertMemberStatus : ", e);
			
			throw e;
		}
	}

	@Override
	public void updatePassword(MemberDto dto) throws Exception {

	}

	@Override
	public void updateMemberEnabled(Map<String, Object> map) throws Exception {
		try {
			mapper.updateMemberEnabled(map);
		} catch (Exception e) {
			log.info("updateMemberEnabled : ", e);
			
			throw e;
		}
	}

	@Override
	public void updateMember(MemberDto dto) throws Exception {

	}

	@Override
	public void updateLastLogin(Long member_id) throws Exception {
		try {
			mapper.updateLastLogin(member_id);
		} catch (Exception e) {
			log.info("updateLastLogin : ", e);
			
			throw e;
		}
	}

	@Override
	public void updateLastLogin(String login_id) throws Exception {
		try {
			mapper.updateLastLoginId(login_id);
		} catch (Exception e) {
			log.info("updateLastLoginId : ", e);
			
			throw e;
		}
	}

	@Override
	public MemberDto findById(Long member_id) {
		MemberDto dto = null;

		try {
			dto = Objects.requireNonNull(mapper.findById(member_id));
		} catch (NullPointerException e) {
		} catch (ArrayIndexOutOfBoundsException e) {
		} catch (Exception e) {
			log.info("findById : ", e);
		}

		return dto;
	}

	@Override
	public MemberDto findById(String login_id) {
		MemberDto dto = null;

		try {
			dto = Objects.requireNonNull(mapper.findByLoginId(login_id));
		} catch (NullPointerException e) {
		} catch (Exception e) {
			log.info("findById : ", e);
		}

		return dto;
	}

	@Override
	public Long getMemberId(String login_id) {
		try {
			Long result = Objects.requireNonNull(mapper.getMemberId(login_id));
			return result;
		} catch (Exception e) {
			log.info("getMemberId : ", e);
		}

		return 0L;
	}
	
	@Override
	public int checkFailureCount(String login_id) {
		int result = 0;
		
		try {
			result = mapper.checkFailureCount(login_id);
		} catch (Exception e) {
			log.info("checkFailureCount : ", e);
		}
		
		return result;
	}

	@Override
	public void updateFailureCountReset(String login_id) throws Exception {
		try {
			mapper.updateFailureCountReset(login_id);
		} catch (Exception e) {
			log.info("updateFailureCountReset : ", e);
			
			throw e;
		}
	}

	@Override
	public void updateFailureCount(String login_id) throws Exception {
		try {
			mapper.updateFailureCount(login_id);
		} catch (Exception e) {
			log.info("updateFailureCount : ", e);
			
			throw e;
		}
	}

	@Override
	public void deleteMember(Map<String, Object> map) throws Exception {

	}

	@Override
	public void deleteProfilePhoto(Map<String, Object> map) throws Exception {

	}

	@Override
	public void generatePwd(MemberDto dto) throws Exception {

	}

	@Override
	public List<MemberDto> listFindMember(Map<String, Object> map) {
		
		return null;
	}

	@Override
	public String findByAuthority(String login_id) {
		String authority = null;
		
		try {
			authority = mapper.findByAuthority(login_id);
		} catch (Exception e) {
			log.info("findByAuthority", e);
		}
		
		return authority;
	}

	@Override
	public boolean isPasswordCheck(String login_id, String password) {
		
		return false;
	}

}
