package com.tk.app.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.mapper.WorkChatRoomMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkChatRoomServiceImpl implements WorkChatRoomService {
	private final WorkChatRoomMapper mapper;
	
	@Transactional
	@Override
	public Long startDirectRoom(String domainKey,Long workId, Long loginMemberId, Long targetMemberId) throws Exception {
		
	        if (workId == null || loginMemberId == null || targetMemberId == null) {
	            throw new ResponseStatusException(
	            		HttpStatus.BAD_REQUEST,"채팅방 생성에 필요한 정보가 없습니다.");
	        }
	        
	        if (domainKey == null || domainKey.isBlank()) {
	            throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "워크 도메인 정보가 없습니다.");
	        }
	
	        if (loginMemberId.equals(targetMemberId)) {
	            throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "자기 자신과는 채팅할 수 없습니다.");
	        }
	        
	        WorkChatMemberDto domaindto = new WorkChatMemberDto();
	        domaindto.setDomainKey(domainKey);
	        domaindto.setWorkId(workId);
	        
	        int count = mapper.matchDomain(domaindto);

	        WorkChatMemberDto mydto = new WorkChatMemberDto();
	        mydto.setWorkId(workId);
	        mydto.setMemberId(loginMemberId);
	
	        Long myWorkId = mapper.findWorkMemberId(mydto);
	        
	        if (count == 0) {
	            throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "요청한 워크 정보가 일치하지 않습니다.");
	        }
	
	        
	        WorkChatMemberDto targetDto = new WorkChatMemberDto();
	        targetDto.setWorkId(workId);
	        targetDto.setMemberId(targetMemberId);
	
	        Long targetWorkId = mapper.findWorkMemberId(targetDto);
	
	        
	        if (myWorkId == null || targetWorkId == null) {
	            throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "워크에 참여 중인 회원만 채팅할 수 있습니다.");
	        }
	

	        WorkChatMemberDto roomdto = new WorkChatMemberDto();
	        roomdto.setWorkId(workId);
	        roomdto.setMyWorkId(myWorkId);
	        roomdto.setTargetWorkId(targetMemberId);
	        
	        Long chatRoomId = mapper.findDirectRoom(roomdto);
	
	        if (chatRoomId != null) {
	            return chatRoomId;
	        }
	
	        chatRoomId = mapper.nextRoomId();
	
	        roomdto.setChatRoomId(chatRoomId);
	
	        mapper.insertDirectRoom(roomdto);
	
	        roomdto.setWorkMemberId(myWorkId);
	        
	        mapper.insertRoomMember(roomdto);
	
	        roomdto.setWorkMemberId(targetMemberId);
	
	        mapper.insertRoomMember(roomdto);
	
	        return chatRoomId;
	        
	    	}

	@Override
	public List<WorkChatMemberDto> findMyDirectRooms(String domainKey, Long loginMemberId) throws Exception {
		 
		try {
			if (domainKey == null || domainKey.isBlank()) {
		        throw new ResponseStatusException(
		            HttpStatus.BAD_REQUEST, "워크 도메인 정보가 없습니다.");
		    }

		    if (loginMemberId == null) {
		        throw new ResponseStatusException(
		            HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		    }

		    WorkChatMemberDto dto = new WorkChatMemberDto();
		    dto.setDomainKey(domainKey);
		    dto.setLoginMemberId(loginMemberId);

		    return mapper.findMyDirectRooms(dto);
			
		} catch (Exception e) {
			throw e;
		}
	}
}
	
	

