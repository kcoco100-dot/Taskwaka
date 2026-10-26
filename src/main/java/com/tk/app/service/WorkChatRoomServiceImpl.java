package com.tk.app.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.chat.WorkChatMessageSavedEvent;
import com.tk.app.domain.dto.WorkChatGroupDto;
import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;
import com.tk.app.mapper.WorkChatGroupMapper;
import com.tk.app.mapper.WorkChatRoomMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkChatRoomServiceImpl implements WorkChatRoomService {
	private final WorkChatRoomMapper mapper;
	private final WorkChatGroupMapper groupmapper;
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

	@Override
	public void markRoomAsRead(WorkChatMessageDto dto) throws Exception {
		try {
		    int count = mapper.countReadableRoom(dto);

		    if (count == 0) {
		        throw new ResponseStatusException(
		            HttpStatus.FORBIDDEN, "접근할 수 없는 채팅방입니다.");
		    }

		    // 로그인 회원의 채팅방 참여자 번호 조회
		    Long roomMemberId = mapper.findSenderRoomMemberId(dto);

		    if (roomMemberId == null) {
		        throw new ResponseStatusException(
		            HttpStatus.FORBIDDEN, "채팅방 참여 정보를 찾을 수 없습니다.");
		    }

		    dto.setChatRoomMemberId(roomMemberId);

		    mapper.updateLastReadMessage(dto);
			
		} catch (Exception e) {
			log.info("markRoomAsRead : ", e);
			throw e;
		}
		
	}
	
	@Override
	@Transactional
	public Long createGroupRoom(WorkChatGroupDto dto) throws Exception {
		try {
		    if (dto == null || dto.getDomainKey() == null || dto.getLoginMemberId() == null
		            || dto.getRoomName() == null || dto.getRoomName().isBlank()
		            || dto.getRoomName().length() > 100 || dto.getMemberIds() == null) {
		        throw new ResponseStatusException(
		                HttpStatus.BAD_REQUEST, "그룹 채팅방 정보가 올바르지 않습니다.");
		    }
		
		    Long workId = groupmapper.findWorkIdByDomain(dto.getDomainKey());
		
		    if (workId == null) {
		        throw new ResponseStatusException(
		                HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
		    }
		
		    WorkChatMemberDto memberDto = new WorkChatMemberDto();
		    memberDto.setWorkId(workId);
		    memberDto.setMemberId(dto.getLoginMemberId());
		
		    Long myWorkId = mapper.findWorkMemberId(memberDto);
		
		    if (myWorkId == null) {
		        throw new ResponseStatusException(
		                HttpStatus.FORBIDDEN, "워크스페이스에 참여한 회원이 아닙니다.");
		    }
		
		    // 초대할 회원 중 중복 제거 (중복 없는 Long형 집합)
		    Set<Long> memberIds = new LinkedHashSet<>(dto.getMemberIds());
		
		    memberIds.remove(dto.getLoginMemberId());
		
		    if (memberIds.contains(null) || memberIds.size() < 2) {
		        throw new ResponseStatusException(
		                HttpStatus.BAD_REQUEST, "그룹 채팅에는 본인 외에 2명 이상 필요합니다.");
		    }
		
		    List<Long> workMemberIds = new ArrayList<>();
		
		    workMemberIds.add(myWorkId);
		
		    for (Long memberId : memberIds) {
		        memberDto.setMemberId(memberId);
		        Long workMemberId = mapper.findWorkMemberId(memberDto);
		        if (workMemberId == null) {
		            throw new ResponseStatusException(
		                    HttpStatus.BAD_REQUEST, "초대할 수 없는 회원이 포함되어 있습니다.");
		        }
		
		        workMemberIds.add(workMemberId);
		    }
		
		    Long chatRoomId = mapper.nextRoomId();
		
		    dto.setChatRoomId(chatRoomId);
		    dto.setWorkId(workId);
		    dto.setMyWorkId(myWorkId);
		
		    groupmapper.insertGroupRoom(dto);
		
		    for (Long workMemberId : workMemberIds) {
		        WorkChatMemberDto roomMemberDto = new WorkChatMemberDto();
		
		        roomMemberDto.setChatRoomId(chatRoomId);
		        roomMemberDto.setWorkMemberId(workMemberId);
		
		        mapper.insertRoomMember(roomMemberDto);
		    }
		    
		    eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
		            chatRoomId,
		            dto.getLoginMemberId(),
		            new ArrayList<>(memberIds)
		    ));
		
		    return chatRoomId;
			
		} catch (Exception e) {
			throw e;
		}

	}



	@Override
	public List<WorkChatMemberDto> findMyGroupRooms(WorkChatMemberDto dto) throws Exception {
		try {
		    List<WorkChatMemberDto> rooms = groupmapper.findMyGroupRooms(dto);
			
		    for (WorkChatMemberDto room : rooms) {
		
		        WorkChatMessageDto last = mapper.findLatestRoomMessage(room);
		
		        if (last != null) {
		            room.setLastMessage(last.getContent());
		            room.setLastSent(last.getSent());
		        }
		
		        room.setLoginMemberId(dto.getLoginMemberId());
		
		        int unreadCount = mapper.countUnreadMessages(room);
		
		        room.setUnreadCount(unreadCount);
		    }
		
		    return rooms;
		    
		} catch (Exception e) {
			throw e;
		}
		
	}


	@Override
	@Transactional
	public String renameGroupRoom(WorkChatGroupDto dto) throws Exception {
	
	    // 1. 기본 요청 정보 검증
	    if (dto == null || dto.getChatRoomId() == null || dto.getChatRoomId() <= 0
	            || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
	            || dto.getLoginMemberId() == null || dto.getRoomName() == null) {	
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "그룹 채팅방 정보가 올바르지 않습니다.");
	    }
	
	    String roomName = dto.getRoomName().trim();
	
	    if (roomName.isEmpty() || roomName.length() > 15) {
	
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "그룹 이름은 1~15자로 입력해 주세요.");
	    }
	
	    dto.setRoomName(roomName);
	
	    int result = groupmapper.updateGroupRoomName(dto);
	
	    if (result != 1) {
	
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "그룹 이름을 변경할 권한이 없습니다.");
	    }
	
	    WorkChatMessageDto messageDto = new WorkChatMessageDto();
	    messageDto.setChatRoomId(dto.getChatRoomId());
	
	    List<Long> memberIds = mapper.findActiveRoomMemberIds(messageDto);
	
	    eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
	            dto.getChatRoomId(),
	            dto.getLoginMemberId(),
	            memberIds
	    ));
	
	    return roomName;
	}


}
	
	

