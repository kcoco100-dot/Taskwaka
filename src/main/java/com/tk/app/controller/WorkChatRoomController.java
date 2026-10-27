package com.tk.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.domain.dto.WorkChatGroupDto;
import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.WorkChatRoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/{domainKey}/api/workchat/rooms/*")
public class WorkChatRoomController {
	private final WorkChatRoomService service;
	
	
	@GetMapping("direct")
	public ResponseEntity<?> findDirectRoom(
			@PathVariable("domainKey") String domainKey) {
		
		try {
			SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
			
			if(loginMember == null) {
			       throw new ResponseStatusException(
			               HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
			}
			
	        Long loginMemberId = loginMember.getMember_id();
	        
	        List<WorkChatMemberDto> rooms = service.findMyDirectRooms(domainKey, loginMemberId);
	        
	        return ResponseEntity.ok(rooms);
			
		} catch (ResponseStatusException e) {
	        log.error("findMyDirectRooms : ", e);

	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());
	        
		} catch (Exception e) {
			log.info("findDirectRoom : ", e);

	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("채팅방 목록 조회 중 오류가 발생했습니다.");
		}
	}
	
	
	@PostMapping("direct")
	public ResponseEntity<?> startDirectRoom(
			@PathVariable("domainKey") String domainKey,
			@RequestBody WorkChatMemberDto dto){
		
		try {
			SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
			if(loginMember == null) {
			       throw new ResponseStatusException(
			               HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
			}
			
			Long loginMemberId = loginMember.getMember_id();
			
			if(dto == null || dto.getWorkId() == null || dto.getMemberId() == null) {
	            throw new ResponseStatusException(
	                    HttpStatus.BAD_REQUEST, "채팅방을 생성할 수 없습니다."
	                    );
	        }
			
			Long workId = dto.getWorkId();
			Long targetMemberId = dto.getMemberId();
			Long chatRoomId = service.startDirectRoom(domainKey, workId, loginMemberId, targetMemberId);
			
			return ResponseEntity.ok(chatRoomId);
			
		} catch (ResponseStatusException e) {
		    log.error("startDirectRoom :", e);

		    return ResponseEntity
		            .status(e.getStatusCode())
		            .body(e.getReason());
		    
		} catch (Exception e) {
		    log.info("startDirectRoom : ", e);

		    return ResponseEntity
		            .status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .body("채팅방 처리 중 오류가 발생했습니다.");
		}	
	}
	

	@GetMapping("{chatRoomId}/messages")
	public ResponseEntity<?> findRoomMessages(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId) {
	
	    try {
	    	SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	        if (loginMember == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .body("로그인이 필요합니다.");
	        }
	
	        Long loginMemberId = loginMember.getMember_id();
	
	        List<WorkChatMessageDto> messages = service.findRoomMessages(domainKey, chatRoomId, loginMemberId);
	
	        
	        return ResponseEntity.ok(messages);
	
	    } catch (ResponseStatusException e) {
	        log.error("findRoomMessages : ", e);
	
	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());
	
	    } catch (Exception e) {
	        log.error("findRoomMessages : ", e);
	
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("메시지 조회 중 오류가 발생했습니다.");
	    }
	}
	

	@PostMapping("{chatRoomId}/messages")
	public ResponseEntity<?> sendMessage(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId,
	        @RequestBody WorkChatMessageDto req) {
	
	    try {
	    	SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	        if (loginMember == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .body("로그인이 필요합니다.");
	        }
	
	        Long loginMemberId =loginMember.getMember_id();
	
	        WorkChatMessageDto dto = new WorkChatMessageDto();
	
	        dto.setDomainKey(domainKey);
	        dto.setChatRoomId(chatRoomId);
	        dto.setLoginMemberId(loginMemberId);
	        dto.setContent(req.getContent());
	
	        service.sendMessage(dto);
	
	        return ResponseEntity.ok("메시지 전송 완료");
	
	    } catch (ResponseStatusException e) {
	        log.error("sendMessage : ", e);
	
	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());
	
	    } catch (Exception e) {
	        log.error("sendMessage : ", e);
	
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("메시지 전송 중 오류가 발생했습니다.");
	    }
	}
	

	@PostMapping("{chatRoomId}/read")
	public ResponseEntity<?> markRoomAsRead(
	        @PathVariable String domainKey,
	        @PathVariable Long chatRoomId,
	        @RequestBody WorkChatMessageDto dto){
		
		try {
			SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
			
			if (loginMember == null) {
		        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		    }
		
		    dto.setDomainKey(domainKey);
		    dto.setChatRoomId(chatRoomId);
		    dto.setLoginMemberId(loginMember.getMember_id());
		
		    service.markRoomAsRead(dto);
		
		    return ResponseEntity.ok().build();
		    
		} catch (ResponseStatusException e) {

	        log.error("markRoomAsRead : ", e);

	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());

	    } catch (Exception e) {

	        log.error("markRoomAsRead : ", e);

	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("메시지 읽음 처리 중 오류가 발생했습니다.");
	    }
	}
	
	
	@GetMapping("group")
	public ResponseEntity<?> findMyGroupRooms(
	        @PathVariable String domainKey) {

	    try {
	    	SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	    	
	        if (loginMember == null) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
	        }

	        WorkChatMemberDto dto = new WorkChatMemberDto();

	        dto.setDomainKey(domainKey);
	        dto.setLoginMemberId(loginMember.getMember_id());

	        return ResponseEntity.ok(service.findMyGroupRooms(dto));

	    } catch (ResponseStatusException e) {
	        return ResponseEntity.status(e.getStatusCode()).body(e.getReason());

	    } catch (Exception e) {
	        log.error("findMyGroupRooms : ", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("그룹 채팅방 조회 중 오류가 발생했습니다.");
	    }
	}
	
	@PostMapping("group")
	public ResponseEntity<?> createGroupRoom(
			@PathVariable("domainKey") String dominKey,
			@RequestBody WorkChatGroupDto dto) {
		try {
			SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
			
			if(loginMember == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			dto.setDomainKey(dominKey);
			dto.setLoginMemberId(loginMember.getMember_id());
			
			Long chatRoomId = service.createGroupRoom(dto);
			
			
			return ResponseEntity.status(HttpStatus.CREATED).body(chatRoomId);
		} catch (Exception e) {
	        log.error("createGroupRoom : ", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("그룹 채팅방 생성 중 오류가 발생했습니다.");
		}
	}
	

	@PutMapping("{chatRoomId}/name")
	public ResponseEntity<?> renameGroupRoom(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId,
	        @RequestBody WorkChatGroupDto dto) {
	
	    try {
	    	SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	
	        if (loginMember == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .build();
	        }
	
	        if (dto == null) {
	            return ResponseEntity
	                    .status(HttpStatus.BAD_REQUEST)
	                    .body("변경할 그룹 이름이 없습니다.");
	        }
	
	        dto.setDomainKey(domainKey);
	        dto.setChatRoomId(chatRoomId);
	        dto.setLoginMemberId(loginMember.getMember_id());
	
	        String newRoomName = service.renameGroupRoom(dto);
	
	        return ResponseEntity.ok(newRoomName);
	
	    } catch (ResponseStatusException e) {
	
	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());
	
	    } catch (Exception e) {
	
	        log.error("renameGroupRoom : ", e);
	
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("그룹 이름 변경 중 오류가 발생했습니다.");
	    }
	}
	
	@GetMapping("{chatRoomId}/members")
	public ResponseEntity<?> findGroupRoomMembers(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId) {

	    try {
	        SessionInfo loginMember = LoginMemberUtil.getSessionInfo();

	        if (loginMember == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .build();
	        }

	        WorkChatMemberDto dto = new WorkChatMemberDto();

	        dto.setDomainKey(domainKey);
	        dto.setChatRoomId(chatRoomId);
	        dto.setLoginMemberId(loginMember.getMember_id());

	        return ResponseEntity.ok(service.findGroupRoomMembers(dto));

	    } catch (ResponseStatusException e) {

	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());

	    } catch (Exception e) {

	        log.error("findGroupRoomMembers : ", e);

	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("그룹 참여자 조회 중 오류가 발생했습니다.");
	    }
	}
	

	@PostMapping("{chatRoomId}/members")
	public ResponseEntity<?> inviteGroupMembers(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId,
	        @RequestBody WorkChatGroupDto dto) {
	
	    try {
	        SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	
	        if (loginMember == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .build();
	        }
	
	        if (dto == null) {
	            return ResponseEntity
	                    .status(HttpStatus.BAD_REQUEST)
	                    .body("초대할 회원 정보가 없습니다.");
	        }
	
	        dto.setDomainKey(domainKey);
	        dto.setChatRoomId(chatRoomId);
	        dto.setLoginMemberId(loginMember.getMember_id());
	
	        service.inviteGroupMembers(dto);
	
	        return ResponseEntity.noContent().build();
	
	    } catch (ResponseStatusException e) {
	
	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());
	
	    } catch (Exception e) {
	
	        log.error("inviteGroupMembers : ", e);
	
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("그룹 회원 초대 중 오류가 발생했습니다.");
	    }
	}
	

	@DeleteMapping("{chatRoomId}/members/out")
	public ResponseEntity<?> leaveGroupRoom(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId) {
	
	    try {
	        SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	
	        if (loginMember == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .build();
	        }
	
	        WorkChatMemberDto dto = new WorkChatMemberDto();
	
	        dto.setDomainKey(domainKey);
	        dto.setChatRoomId(chatRoomId);
	        dto.setLoginMemberId(loginMember.getMember_id());
	
	        service.leaveGroupRoom(dto);
	
	        return ResponseEntity.noContent().build();
	
	    } catch (ResponseStatusException e) {
	
	        return ResponseEntity
	                .status(e.getStatusCode())
	                .body(e.getReason());
	
	    } catch (Exception e) {
	
	        log.error("leaveGroupRoom : ", e);
	
	        return ResponseEntity
	                .status(HttpStatus.INTERNAL_SERVER_ERROR)
	                .body("채팅방 나가기 중 오류가 발생했습니다.");
	    }
	}



	

}
