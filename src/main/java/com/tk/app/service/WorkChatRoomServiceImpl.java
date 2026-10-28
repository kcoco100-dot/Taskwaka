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
	
	
	private String getRoleName(Integer role) {

	    if (role == null) return "MEMBER";

	    return switch (role) {
	        case 70 -> "사장";
	        case 50 -> "과장";
	        case 30 -> "대리";
	        case 10 -> "사원";
	        default -> "인턴";
	    };
	}
	
	private void lockChatRoom(Long chatRoomId) {

	    Long lockedRoomId = mapper.lockChatRoom(chatRoomId);

	    if (lockedRoomId == null) {
	        throw new ResponseStatusException(
	            HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."
	        );
	    }
	}
	
	private WorkChatMemberDto checkGroupRoomAccess(
	        WorkChatMemberDto dto) throws Exception {
	
	    // 1. 요청 데이터 확인
	    if (dto == null || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
	        || dto.getChatRoomId() == null || dto.getChatRoomId() <= 0 || dto.getLoginMemberId() == null) {
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
	                HttpStatus.FORBIDDEN, "워크스페이스 접근 권한이 없습니다.");
	    }
	

	    List<WorkChatMemberDto> rooms =groupmapper.findMyGroupRooms(dto);
	
	    WorkChatMemberDto room = rooms.stream()
	            .filter(item -> dto.getChatRoomId().equals(item.getChatRoomId()))
	            .findFirst()
	            .orElseThrow(() ->
	                    new ResponseStatusException(
	                            HttpStatus.FORBIDDEN, "접근할 수 없는 그룹 채팅방입니다."));
	    
	    room.setMyWorkId(myWorkId);
	
	    return room;
	}
	
	private void saveSystemMessage(
			Long chatRoomId,
	        Long authorRoomMemberId,
	        String content) {

	    WorkChatMessageDto dto = new WorkChatMessageDto();

	    dto.setChatRoomId(chatRoomId);
	    dto.setChatRoomMemberId(authorRoomMemberId);
	    dto.setContent(content);

	    int result = mapper.insertSystemRoomMessage(dto);

	    if (result != 1) {
	        throw new IllegalStateException(
	                "시스템 메시지를 저장하지 못했습니다.");
	    }
	}
	
	private String findGroupMemberName(
	        List<WorkChatMemberDto> members,
	        Long memberId) {

	    return members.stream()
	            .filter(member -> memberId.equals(member.getMemberId()))
	            .map(WorkChatMemberDto::getName)
	            .filter(name -> name != null && !name.isBlank())
	            .findFirst()
	            .orElse("회원");
	}

	

	@Transactional
	@Override
	public Long startDirectRoom(String domainKey, Long workId,
	        Long loginMemberId, Long targetMemberId) throws Exception {
	
	    if (domainKey == null || domainKey.isBlank() || workId == null
	            || loginMemberId == null || targetMemberId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "채팅방 정보가 올바르지 않습니다.");
	    }
	
	    if (loginMemberId.equals(targetMemberId)) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "자기 자신과 채팅할 수 없습니다.");
	    }
	
	    WorkChatMemberDto domainDto = new WorkChatMemberDto();
	    domainDto.setDomainKey(domainKey);
	    domainDto.setWorkId(workId);
	
	    if (mapper.matchDomain(domainDto) == 0) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "워크 정보가 일치하지 않습니다.");
	    }
	
	    WorkChatMemberDto memberDto = new WorkChatMemberDto();
	    memberDto.setWorkId(workId);
	
	    memberDto.setMemberId(loginMemberId);
	    Long myWorkId = mapper.findWorkMemberId(memberDto);
	
	    memberDto.setMemberId(targetMemberId);
	    Long targetWorkId = mapper.findWorkMemberId(memberDto);
	
	    if (myWorkId == null || targetWorkId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "활성 워크 멤버만 채팅할 수 있습니다.");
	    }
	
	    // 4. 두 멤버 행을 항상 같은 순서로 잠금
	    Long first = Math.min(myWorkId, targetWorkId);
	    Long second = Math.max(myWorkId, targetWorkId);
	
	    if (mapper.lockActiveDirectWorkMember(first) == null 
	    		|| mapper.lockActiveDirectWorkMember(second) == null) {
	        throw new ResponseStatusException(
	                HttpStatus.CONFLICT, "워크 멤버 상태가 변경되었습니다.");
	    }
	
	    WorkChatMemberDto roomDto = new WorkChatMemberDto();
	    roomDto.setWorkId(workId);
	    roomDto.setMyWorkId(myWorkId);
	    roomDto.setTargetWorkId(targetWorkId);
	
	    Long chatRoomId = mapper.findDirectRoom(roomDto);
	
	    if (chatRoomId != null) {
	
	        Long lockedRoomId = mapper.lockChatRoom(chatRoomId);
	
	        if (lockedRoomId == null) {
	            throw new ResponseStatusException(
	                    HttpStatus.CONFLICT, "채팅방 상태가 변경되었습니다. 다시 시도해주세요.");
	        }
	
	        roomDto.setChatRoomId(chatRoomId);
	        roomDto.setWorkMemberId(myWorkId);
	
	        Integer status = mapper.findDirectRoomMemberStatus(roomDto);
	
	        if (Integer.valueOf(0).equals(status)) {
	            return chatRoomId;
	        }
	
	        if (! Integer.valueOf(1).equals(status)) {
	            throw new ResponseStatusException(
	                    HttpStatus.CONFLICT, "채팅방에 참여 하고있지 않습니다.");
	        }
	
	        Long lastMessageId = mapper.findLatestRoomMessageId(roomDto);
	
	        roomDto.setVisibleAfterMessageId(lastMessageId == null ? 0L : lastMessageId);
	
	        int updated = mapper.reactivateDirectRoomMember(roomDto);
	
	        if (updated != 1) {
	            throw new ResponseStatusException(
	                    HttpStatus.CONFLICT, "채팅방 재참여에 실패했습니다.");
	        }
	
	        WorkChatMessageDto messageDto = new WorkChatMessageDto();
	
	        messageDto.setChatRoomId(chatRoomId);
	        messageDto.setLoginMemberId(loginMemberId);
	
	        Long roomMemberId = mapper.findSenderRoomMemberId(messageDto);
	
	        if (roomMemberId == null) {
	            throw new IllegalStateException("재참여 회원 정보를 찾을 수 없습니다.");
	        }

	        String memberName = mapper.findDirectRoomMemberName(roomDto);
	
	        saveSystemMessage(
	                chatRoomId,
	                roomMemberId,
	                memberName + "님이 채팅방에 다시 참여했습니다."
	        );
	
	        List<Long> activeMemberIds = mapper.findActiveRoomMemberIds(messageDto);
	
	        eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
	                        chatRoomId,
	                        loginMemberId,
	                        activeMemberIds
	                )
	        );
	
	        return chatRoomId;
	    }
	
	    chatRoomId = mapper.nextRoomId();
	
	    roomDto.setChatRoomId(chatRoomId);
	
	    mapper.insertDirectRoom(roomDto);
	
	    roomDto.setWorkMemberId(myWorkId);
	    mapper.insertRoomMember(roomDto);
	
	    roomDto.setWorkMemberId(targetWorkId);
	    mapper.insertRoomMember(roomDto);
	
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
			
				room.setLoginMemberId(loginMemberId);
				
			    WorkChatMessageDto latest = mapper.findLatestRoomMessage(room);
			
			    if (latest != null) {
			        room.setLastMessage(latest.getContent());
			        room.setLastSent(latest.getSent());
			    }
			    

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
		    
		    lockChatRoom(dto.getChatRoomId());
		    
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
	@Transactional
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
			            HttpStatus.FORBIDDEN, "워크스페이스 접근 권한이 없습니다.");
			}
			
		    List<WorkChatMemberDto> rooms = groupmapper.findMyGroupRooms(dto);
			
		    for (WorkChatMemberDto room : rooms) {
		    	
		    	room.setLoginMemberId(dto.getLoginMemberId());
		    	
		        boolean isOwner = myWorkId.equals(room.getOwnerWorkMemberId());

		        room.setCanManage(isOwner ? 1 : 0);
		
		        WorkChatMessageDto last = mapper.findLatestRoomMessage(room);
		
		        if (last != null) {
		            room.setLastMessage(last.getContent());
		            room.setLastSent(last.getSent());
		        }
		
		        int unreadCount = mapper.countUnreadMessages(room);
		
		        room.setUnreadCount(unreadCount);
		    }
		
		    return rooms;
		    
		} catch (Exception e) {
			throw e;
		}
		
	}
	

	@Override
	public List<WorkChatMemberDto> findGroupRoomMembers(WorkChatMemberDto dto) throws Exception {
	
	    WorkChatMemberDto room = checkGroupRoomAccess(dto);
	
	    List<WorkChatMemberDto> members = groupmapper.findGroupRoomMembers(dto.getChatRoomId());
	
	    for (WorkChatMemberDto member : members) {
	        member.setRoleName(getRoleName(member.getRole()));
	
	        boolean isOwner =room.getOwnerWorkMemberId().equals(member.getWorkMemberId());
	        member.setCanManage(isOwner ? 1 : 0);
	    }
	
	    members.sort((a, b) ->
	            Integer.compare(
	            b.getCanManage(),
	            a.getCanManage()));
	
	    return members;
	}


	@Override
	@Transactional
	public String renameGroupRoom(WorkChatGroupDto dto) throws Exception {
	
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
	    
	    lockChatRoom(dto.getChatRoomId());
	    
	    WorkChatMemberDto accessDto = new WorkChatMemberDto();

	    accessDto.setDomainKey(dto.getDomainKey());
	    accessDto.setChatRoomId(dto.getChatRoomId());
	    accessDto.setLoginMemberId(dto.getLoginMemberId());

	    WorkChatMemberDto room = checkGroupRoomAccess(accessDto);

	    if (! room.getMyWorkId().equals(room.getOwnerWorkMemberId())) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "그룹 생성자만 이름을 변경할 수 있습니다.");
	    }

	    dto.setMyWorkId(room.getMyWorkId());
	
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
	
	

	@Override
	@Transactional
	public void inviteGroupMembers(WorkChatGroupDto dto)
	        throws Exception {
	
	    if (dto == null || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
	        || dto.getChatRoomId() == null || dto.getChatRoomId() <= 0 || dto.getLoginMemberId() == null
	        || dto.getMemberIds() == null || dto.getMemberIds().isEmpty()) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "초대할 회원 정보가 올바르지 않습니다.");
	    }

	    WorkChatMemberDto accessDto = new WorkChatMemberDto();
	
	    accessDto.setDomainKey(dto.getDomainKey());
	    accessDto.setChatRoomId(dto.getChatRoomId());
	    accessDto.setLoginMemberId(dto.getLoginMemberId());
	    
	    lockChatRoom(dto.getChatRoomId());
	
	    WorkChatMemberDto room = checkGroupRoomAccess(accessDto);
	
	    if (! room.getMyWorkId().equals( room.getOwnerWorkMemberId())) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "그룹 생성자만 회원을 초대할 수 있습니다.");
	    }
	
	    Set<Long> memberIds = new LinkedHashSet<>(dto.getMemberIds());
	
	    if (memberIds.contains(null) || memberIds.contains(dto.getLoginMemberId())) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "초대 대상에 올바르지 않은 회원이 포함되어 있습니다.");
	    }
	
	    Long workId = groupmapper.findWorkIdByDomain(dto.getDomainKey());
	
	    if (workId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
	    }
	
	    WorkChatMemberDto memberDto = new WorkChatMemberDto();
	    
	    memberDto.setWorkId(workId);
	    memberDto.setChatRoomId(dto.getChatRoomId());
	    
	    Long lastMessageId = mapper.findLatestRoomMessageId(memberDto);

	    memberDto.setVisibleAfterMessageId(lastMessageId);
	
	    for (Long memberId : memberIds) {
	
	        memberDto.setMemberId(memberId);
	
	        Long workMemberId = mapper.findWorkMemberId(memberDto);
	
	        if (workMemberId == null) {
	            throw new ResponseStatusException(
	                    HttpStatus.BAD_REQUEST, "현재 워크에 속하지 않은 회원이 포함되어 있습니다.");
	        }
	
	        memberDto.setWorkMemberId(workMemberId);
	
	        Integer status = groupmapper.findRoomMemberStatus(memberDto);
	
	        if (status != null && status == 0) {
	            throw new ResponseStatusException(
	                    HttpStatus.CONFLICT, "이미 그룹 채팅에 참여 중인 회원이 포함되어 있습니다.");
	        }
	
	        if (status == null) {
	            mapper.insertRoomMember(memberDto);
	        } else {
	            int updated = groupmapper.reactivateRoomMember(memberDto);
	            
	            if (updated != 1) {
	                throw new ResponseStatusException(
	                        HttpStatus.CONFLICT, "회원의 재참여 처리에 실패했습니다.");
	            }
	        }
	    }
	    
	    WorkChatMessageDto authorDto = new WorkChatMessageDto();

	    authorDto.setChatRoomId(dto.getChatRoomId());
	    authorDto.setLoginMemberId(dto.getLoginMemberId());

	    Long authorRoomMemberId = mapper.findSenderRoomMemberId(authorDto);

	    if (authorRoomMemberId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "시스템 메시지 작성자 정보를 찾을 수 없습니다.");
	    }

	    List<WorkChatMemberDto> groupMembers = groupmapper.findGroupRoomMembers(dto.getChatRoomId());

	    String inviterName = findGroupMemberName(groupMembers, dto.getLoginMemberId());

	    for (Long memberId : memberIds) {
	        String invitedName = findGroupMemberName(groupMembers, memberId);
	        saveSystemMessage(
	                dto.getChatRoomId(),
	                authorRoomMemberId,
	                inviterName + "님이 " + invitedName + "님을 초대했습니다."
	        );
	    }
	    
	    WorkChatMessageDto messageDto = new WorkChatMessageDto();
	    messageDto.setChatRoomId(dto.getChatRoomId());
	
	    List<Long> activeMemberIds = mapper.findActiveRoomMemberIds(messageDto);
	
	    eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
	                    dto.getChatRoomId(),
	                    dto.getLoginMemberId(),
	                    activeMemberIds
	            	)
	    );
	}
	

	@Override
	@Transactional
	public void leaveGroupRoom(WorkChatMemberDto dto)
	        throws Exception {

	    if (dto == null || dto.getChatRoomId() == null || dto.getChatRoomId() <= 0) {
	        throw new ResponseStatusException(
	            HttpStatus.BAD_REQUEST, "채팅방 정보가 올바르지 않습니다."
	        );
	    }

	    lockChatRoom(dto.getChatRoomId());
	
	    WorkChatMemberDto room = checkGroupRoomAccess(dto);
	
	    if (room.getMyWorkId().equals(room.getOwnerWorkMemberId())) {
	
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "그룹 생성자는 채팅방을 나갈 수 없습니다.");
	    }
	    
	    WorkChatMessageDto authorDto = new WorkChatMessageDto();

	    authorDto.setChatRoomId(dto.getChatRoomId());
	    authorDto.setLoginMemberId(dto.getLoginMemberId());

	    Long authorRoomMemberId = mapper.findSenderRoomMemberId(authorDto);

	    if (authorRoomMemberId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "채팅방 참여 정보를 찾을 수 없습니다.");
	    }

	    List<WorkChatMemberDto> groupMembers = groupmapper.findGroupRoomMembers(dto.getChatRoomId());

	    String leavingName = findGroupMemberName(groupMembers, dto.getLoginMemberId());
	
	    WorkChatMessageDto messageDto = new WorkChatMessageDto();
	    messageDto.setChatRoomId(dto.getChatRoomId());
	
	    List<Long> memberIds = mapper.findActiveRoomMemberIds(messageDto);
	
	    dto.setWorkMemberId(room.getMyWorkId());
	
	    int result = groupmapper.leaveGroupRoom(dto);
	
	    if (result != 1) {
	        throw new ResponseStatusException(
	                HttpStatus.CONFLICT, "채팅방 나가기 처리에 실패했습니다.");
	    }

	    saveSystemMessage(
	            dto.getChatRoomId(),
	            authorRoomMemberId,
	            leavingName + "님이 채팅방을 나갔습니다."
	    );
	    
	    eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
	                    dto.getChatRoomId(),
	                    dto.getLoginMemberId(),
	                    memberIds
	            )
	    );
	}
	
	@Override
	@Transactional
	public void transferGroupMaster(WorkChatMemberDto dto) throws Exception {

	    if (dto == null || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
	        || dto.getChatRoomId() == null || dto.getChatRoomId() <= 0 || dto.getLoginMemberId() == null
	        || dto.getTargetMemberId() == null || dto.getTargetMemberId() <= 0) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "마스터 위임 정보가 올바르지 않습니다.");
	    }

	    if (dto.getLoginMemberId().equals(dto.getTargetMemberId())) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "자기 자신에게 마스터 권한을 위임할 수 없습니다.");
	    }

	    lockChatRoom(dto.getChatRoomId());

	    WorkChatMemberDto room = checkGroupRoomAccess(dto);

	    if (!room.getMyWorkId().equals(room.getOwnerWorkMemberId())) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "현재 마스터만 권한을 위임할 수 있습니다.");
	    }

	    Long workId = groupmapper.findWorkIdByDomain(dto.getDomainKey());

	    if (workId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND, "워크스페이스를 찾을 수 없습니다.");
	    }

	    WorkChatMemberDto targetDto = new WorkChatMemberDto();

	    targetDto.setWorkId(workId);
	    targetDto.setMemberId(dto.getTargetMemberId());

	    Long targetWorkId = mapper.findWorkMemberId(targetDto);

	    if (targetWorkId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "위임 대상이 워크스페이스에 참여 중이지 않습니다.");
	    }

	    dto.setMyWorkId(room.getMyWorkId());
	    dto.setTargetWorkId(targetWorkId);

	    int result = groupmapper.transferGroupMaster(dto);

	    if (result != 1) {
	        throw new ResponseStatusException(
	                HttpStatus.CONFLICT, "마스터 권한 위임에 실패했습니다. 대상 회원의 참여 상태를 확인해 주세요.");
	    }
	    
	 // 시스템 메시지 작성자 조회
	    WorkChatMessageDto authorDto = new WorkChatMessageDto();

	    authorDto.setChatRoomId(dto.getChatRoomId());
	    authorDto.setLoginMemberId(dto.getLoginMemberId());

	    Long authorRoomMemberId =
	            mapper.findSenderRoomMemberId(authorDto);

	    if (authorRoomMemberId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN,
	                "채팅방 참여 정보를 찾을 수 없습니다.");
	    }

	    List<WorkChatMemberDto> groupMembers = groupmapper.findGroupRoomMembers(dto.getChatRoomId());

	    String previousMasterName = findGroupMemberName(groupMembers, dto.getLoginMemberId());

	    String newMasterName = findGroupMemberName(groupMembers, dto.getTargetMemberId());

	    saveSystemMessage(
	            dto.getChatRoomId(),
	            authorRoomMemberId,
	            previousMasterName + "님이 " + newMasterName + "님에게 마스터 권한을 위임했습니다.");

	    WorkChatMessageDto messageDto = new WorkChatMessageDto();
	    messageDto.setChatRoomId(dto.getChatRoomId());

	    List<Long> memberIds = mapper.findActiveRoomMemberIds(messageDto);

	    eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
	                    dto.getChatRoomId(),
	                    dto.getLoginMemberId(),
	                    memberIds
	            )
	    );
	}
	
	@Override
	@Transactional
	public void leaveDirectRoom(WorkChatMemberDto dto) throws Exception {
	    if (dto == null || dto.getDomainKey() == null || dto.getDomainKey().isBlank()
	        || dto.getChatRoomId() == null || dto.getChatRoomId() <= 0 || dto.getLoginMemberId() == null) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST, "채팅방 정보가 올바르지 않습니다.");
	    }
	    Long chatRoomId = dto.getChatRoomId();

	    Long workId = groupmapper.findWorkIdByDomain(dto.getDomainKey());
	
	    if (workId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND, "워크를 찾을 수 없습니다.");
	    }
	
	    WorkChatMemberDto memberDto = new WorkChatMemberDto();
	
	    memberDto.setWorkId(workId);
	    memberDto.setMemberId(dto.getLoginMemberId());
	
	    Long myWorkId = mapper.findWorkMemberId(memberDto);
	
	    if (myWorkId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "워크 접근 권한이 없습니다.");
	    }

	    lockChatRoom(chatRoomId);
	
	    dto.setWorkMemberId(myWorkId);

	    Integer status = mapper.findDirectRoomMemberStatus(dto);
	
	    if (!Integer.valueOf(0).equals(status)) {
	        throw new ResponseStatusException(
	                HttpStatus.FORBIDDEN, "현재 참여 중인 1:1 채팅방이 아닙니다.");
	    }
	
	    WorkChatMessageDto messageDto = new WorkChatMessageDto();
	
	    messageDto.setChatRoomId(chatRoomId);
	    messageDto.setLoginMemberId(dto.getLoginMemberId());
	
	    Long roomMemberId = mapper.findSenderRoomMemberId(messageDto);
	
	    if (roomMemberId == null) {
	        throw new ResponseStatusException(
	                HttpStatus.CONFLICT, "채팅방 참여 정보를 찾을 수 없습니다.");
	    }
	
	    String memberName = mapper.findDirectRoomMemberName(dto);
	
	    if (memberName == null || memberName.isBlank()) {
	        memberName = "UNKNOWN";
	    }

	    int updated = mapper.leaveDirectRoomMember(dto);
	
	    if (updated != 1) {
	        throw new ResponseStatusException(
	                HttpStatus.CONFLICT, "채팅방 나가기 처리에 실패했습니다.");
	    }

	    int remaining = mapper.countActiveDirectRoomMembers(chatRoomId);

	    if (remaining == 0) {
	        mapper.deleteDirectRoomMessages(chatRoomId);
	
	        mapper.deleteDirectRoomMembers(chatRoomId);
	
	        int deleted = mapper.deleteDirectRoom(chatRoomId);
	
	        if (deleted != 1) {
	            throw new IllegalStateException("채팅방 삭제에 실패했습니다.");
	        }
	
	        return;
	    }
	
	    saveSystemMessage(
	            chatRoomId,
	            roomMemberId,
	            memberName + "님이 채팅방을 나갔습니다."
	    );
	
	    List<Long> activeMemberIds = mapper.findActiveRoomMemberIds(messageDto);
	
	    eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
	                    chatRoomId,
	                    dto.getLoginMemberId(),
	                    activeMemberIds
	            )
	    );
	}


}
	
	

