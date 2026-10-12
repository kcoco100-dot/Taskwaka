package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {
	private final MemberMapper mapper;
	private final PasswordEncoder passwordEncoder;
	
	@Value("${file.upload-root}/member")
	private String uploadPath;
	
	@Override
	public MemberDto loginSnsMember(Map<String, Object> map) {

		return null;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void insertMember(MemberDto dto) throws Exception {
		String encodedPassword = passwordEncoder.encode(dto.getPassword());
		
		dto.setPassword(encodedPassword);
		dto.setRole("USER");
		
		Long memberId = mapper.memberSeq();
		dto.setMember_id(memberId);
		
		mapper.insertMember1(dto);
		mapper.insertMember2(dto);
		mapper.insertAuthority(dto);
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
	    return mapper.listFindMember(map);
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

	@Override
	public Integer countByLoginId(String login_id) {
		return mapper.countByLoginId(login_id);
	}

	@Override
	public MemberDto findByLoginIdAndEmail(Map<String, Object> map) {
		return mapper.findByLoginIdAndEmail(map);
	}

	@Override
	public int insertPasswordResetToken(Map<String, Object> map) {
		return mapper.insertPasswordResetToken(map);
	}

	@Override
	public Long findResetMemberId(Map<String, Object> map) {
		return mapper.findResetMemberId(map);
	}

	@Override
	public int useResetToken(Map<String, Object> map) {
		return mapper.useResetToken(map);
	}


	@Transactional
	@Override
	public boolean resetPassword(String tokenHash, String newPassword) {
	
	    Map<String, Object> map = Map.of("token_hash", tokenHash);
	
	    Long memberId = mapper.findResetMemberId(map);
	
	    if (memberId == null) {
	        return false;
	    }
	
	    if (mapper.useResetToken(map) != 1) {
	        return false;
	    }
	
	    MemberDto dto = new MemberDto();
	    dto.setMember_id(memberId);
	    dto.setPassword(passwordEncoder.encode(newPassword));
	
	    try {
	        mapper.updateMemberPassword(dto);
	    } catch (SQLException e) {
	        throw new IllegalStateException("비밀번호 변경 실패", e);
	    }
	
	    return true;
	}


}
