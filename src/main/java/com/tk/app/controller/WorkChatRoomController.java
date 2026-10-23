package com.tk.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.WorkChatMemberDto;
import com.tk.app.domain.dto.WorkChatMessageDto;
import com.tk.app.security.CustomUserDetails;
import com.tk.app.service.WorkChatRoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/{domainKey}/api/workchat/rooms")
public class WorkChatRoomController {
	private final WorkChatRoomService service;
	
	@GetMapping("/direct")
	public ResponseEntity<?> findDirectRoom(
			@PathVariable("domainKey") String domainKey,
			@AuthenticationPrincipal CustomUserDetails customUserDetails) {
		
		try {
			if(customUserDetails == null) {
			       throw new ResponseStatusException(
			               HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
			}
			
	        Long loginMemberId = customUserDetails.getMember().getMember_id();
	        
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
	
	
	@PostMapping("/direct")
	public ResponseEntity<?> startDirectRoom(
			@PathVariable("domainKey") String domainKey,
			@RequestBody WorkChatMemberDto dto,
			@AuthenticationPrincipal CustomUserDetails customUserDetails){
		
		try {
			if(customUserDetails == null) {
			       throw new ResponseStatusException(
			               HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
			}
			
			Long loginMemberId = customUserDetails.getMember().getMember_id();
			
			if(dto == null || dto.getWorkId() == null || dto.getMemberId() == null) {
	            throw new ResponseStatusException(
	                    HttpStatus.BAD_REQUEST, "채팅방을 생성할 수 없습니다."
	                    );
	        }
			
			Long workId = dto.getWorkId();
			Long targetMemberId = dto.getMemberId();
			Long chatRoomId = service.startDirectRoom(domainKey,workId, loginMemberId, targetMemberId);
			
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
	

	@GetMapping("/{chatRoomId}/messages")
	public ResponseEntity<?> findRoomMessages(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId,
	        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
	
	    try {
	        if (customUserDetails == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .body("로그인이 필요합니다.");
	        }
	
	        Long loginMemberId = customUserDetails.getMember().getMember_id();
	
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
	

	@PostMapping("/{chatRoomId}/messages")
	public ResponseEntity<?> sendMessage(
	        @PathVariable("domainKey") String domainKey,
	        @PathVariable("chatRoomId") Long chatRoomId,
	        @RequestBody WorkChatMessageDto req,
	        @AuthenticationPrincipal CustomUserDetails customUserDetails) {
	
	    try {
	        if (customUserDetails == null) {
	            return ResponseEntity
	                    .status(HttpStatus.UNAUTHORIZED)
	                    .body("로그인이 필요합니다.");
	        }
	
	        Long loginMemberId = customUserDetails.getMember().getMember_id();
	
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
	

	@PostMapping("/{chatRoomId}/read")
	public ResponseEntity<?> markRoomAsRead(
	        @PathVariable String domainKey,
	        @PathVariable Long chatRoomId,
	        @RequestBody WorkChatMessageDto dto,
	        @AuthenticationPrincipal CustomUserDetails customUserDetails){
		try {
			if (customUserDetails == null) {
		        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		    }
		
		    dto.setDomainKey(domainKey);
		    dto.setChatRoomId(chatRoomId);
		    dto.setLoginMemberId(customUserDetails.getMember().getMember_id());
		
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

}
