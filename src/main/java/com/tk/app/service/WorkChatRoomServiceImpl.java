package com.tk.app.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.chat.WorkChatMessageSavedEvent;
import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;
import com.tk.app.mapper.WorkChatRoomMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkChatRoomServiceImpl implements WorkChatRoomService {
	private final WorkChatRoomMapper mapper;
	private final ApplicationEventPublisher eventPublisher;
	
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
	        roomdto.setTargetWorkId(targetWorkId);
	        
	        Long chatRoomId = mapper.findDirectRoom(roomdto);
	
	        if (chatRoomId != null) {
	            return chatRoomId;
	        }
	
	        chatRoomId = mapper.nextRoomId();
	
	        roomdto.setChatRoomId(chatRoomId);
	
	        mapper.insertDirectRoom(roomdto);
	
	        roomdto.setWorkMemberId(myWorkId);
	        
	        mapper.insertRoomMember(roomdto);
	
	        roomdto.setWorkMemberId(targetWorkId);
	
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

			List<WorkChatMemberDto> rooms = mapper.findMyDirectRooms(dto);
			
			for (WorkChatMemberDto room : rooms) {
			
			    WorkChatMessageDto latest = mapper.findLatestRoomMessage(room);
			
			    if (latest != null) {
			        room.setLastMessage(latest.getContent());
			        room.setLastSent(latest.getSent());
			    }
			    
			    room.setLoginMemberId(loginMemberId);

			    int unreadCount = mapper.countUnreadMessages(room);

			    room.setUnreadCount(unreadCount);
			}
			
			rooms.sort((a, b) -> {

			    String dateA = a.getLastSent();
			    String dateB = b.getLastSent();

			    // 두 채팅방 모두 메시지가 없는 경우
			    if (dateA == null && dateB == null) return 0;

			    // 메시지가 없는 채팅방은 아래로
			    if (dateA == null) return 1;
			    if (dateB == null) return -1;

			    // 최근 메시지부터 정렬
			    return dateB.compareTo(dateA);
			});

			return rooms;

		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public List<WorkChatMessageDto> findRoomMessages(String domainKey, Long chatRoomId, Long loginMemberId)
			throws Exception {
		try {
		    if (domainKey == null || domainKey.isBlank() || chatRoomId == null || chatRoomId <= 0) {
		        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "메시지 조회에 필요한 정보가 없습니다.");
		    }

		    if (loginMemberId == null) {
		        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
		    }

		    WorkChatMessageDto dto = new WorkChatMessageDto();

		    dto.setDomainKey(domainKey);
		    dto.setChatRoomId(chatRoomId);
		    dto.setLoginMemberId(loginMemberId);

		    int count = mapper.countReadableRoom(dto);

		    if (count == 0) {
		        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근할 수 없는 채팅방입니다.");
		    }

			List<WorkChatMessageDto> messages = mapper.findRoomMessages(dto);
			
			for (WorkChatMessageDto message : messages) {
			
			    if (loginMemberId.equals(message.getSenderMemberId())) {
			        message.setMine(1);
			    } else {
			        message.setMine(0);
			    }
			}
			
			return messages;

			
		} catch (Exception e) {
			log.error("findRoomMessages : ", e);
			throw e;
		}
		
	}

	@Override
	@Transactional
	public void sendMessage(WorkChatMessageDto dto) throws Exception {
	    try {
	    	if (dto == null || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
		    		|| dto.getChatRoomId() == null || dto.getChatRoomId() <= 0) {
		        throw new ResponseStatusException(
		        		HttpStatus.BAD_REQUEST, "채팅방 정보가 올바르지 않습니다."
		        );
		    }
		
		    if (dto.getLoginMemberId() == null) {
		        throw new ResponseStatusException(
		            HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."
		        );
		    }
		
		    String content = dto.getContent();
		
		    if (content == null || content.isBlank()) {
		        throw new ResponseStatusException(
		            HttpStatus.BAD_REQUEST, "메시지 내용을 입력해주세요."
		        );
		    }
		
		    dto.setContent(content.trim());
		    
			int count = mapper.countReadableRoom(dto);
		
			if (count == 0) {
			    throw new ResponseStatusException(
			         HttpStatus.FORBIDDEN, "접근할 수 없는 채팅방입니다."
			    );
			}
		
			 Long chatRoomMemberId = mapper.findSenderRoomMemberId(dto);
		
			 if (chatRoomMemberId == null) {
			     throw new ResponseStatusException(
			         HttpStatus.FORBIDDEN, "채팅방 참여 정보를 찾을 수 없습니다."
			     );
			 }
		
			 dto.setChatRoomMemberId(chatRoomMemberId);
		
			 int result = mapper.insertRoomMessage(dto);
		
			 if (result != 1) {
			     throw new IllegalStateException("메시지를 저장하지 못했습니다.");
			 }
			 

			List<Long> memberIds =
			        mapper.findActiveRoomMemberIds(dto);

			
			eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
			        dto.getChatRoomId(),
			        dto.getLoginMemberId(),
			        memberIds
			    )
			);


			 
		} catch (Exception e) {
			log.info("sendMessage : ", e);
			throw e;
		}
		
	}
}
	
	

