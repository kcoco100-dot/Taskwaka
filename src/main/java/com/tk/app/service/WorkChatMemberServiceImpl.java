package com.tk.app.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.mapper.WorkChatMemberMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkChatMemberServiceImpl implements WorkChatMemberService {
	
	private final WorkChatMemberMapper mapper;
	
	@Override
	public List<WorkChatMemberDto> getMembers(Long workId, Long loginMemberId) {
		
		if(loginMemberId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
			
		}
		
		if(workId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "가입된 워크를 찾을 수 없습니다.");
		}
		
		List<WorkChatMemberDto> members = mapper.findMemberByWorkId(workId);
		
		boolean isMember = false;
		
		for(WorkChatMemberDto member : members) {
			if(loginMemberId.equals(member.getMemberId())) {
				isMember = true;
			}
			
			Integer role = member.getRole();
			
			if(role == null) {
				member.setRoleName("MEMBER");
				continue;
			}
			
			switch(role) {
			case 70 -> member.setRoleName("PM");
			case 50 -> member.setRoleName("DT");
			case 30 -> member.setRoleName("TL");
			case 10 -> member.setRoleName("AA");
			default -> member.setRoleName("MEMBER");
			}
			
				
		}
		
		if(! isMember) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 워크 접근 권한이 없습니다.");	
		}
		
		
		return members;
	}

}
