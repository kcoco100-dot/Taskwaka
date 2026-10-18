
package com.tk.app.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import com.tk.app.admin.domain.dto.AdminPmChangeDto;
import com.tk.app.admin.service.AdminPmChangeService;
import com.tk.app.mapper.MemberMapper;

@RestController
@RequestMapping("/admin/api/pm-changes")
@RequiredArgsConstructor
public class AdminPmChangeApiController {

    private final AdminPmChangeService service;
    private final MemberMapper memberMapper;

    @GetMapping
    public ResponseEntity<List<AdminPmChangeDto>> findAllPmChanges() {
        return ResponseEntity.ok(service.findAllPmChanges());
    }
    

	@PostMapping("/process")
	public ResponseEntity<String> processPmChange(
	        @RequestBody AdminPmChangeDto dto,
	        Authentication authentication) {
	
	    if (dto.getPm_change_id() <= 0) {
	        return ResponseEntity.badRequest().body("잘못된 요청번호입니다.");
	    }
	
	    if (dto.getStatus() != 1 && dto.getStatus() != 2) {
	        return ResponseEntity.badRequest().body("잘못된 처리 상태입니다.");
	    }
	
	    if (dto.getStatus() == 2 &&
	            (dto.getReject_reason() == null ||
	             dto.getReject_reason().trim().isEmpty())) {
	        return ResponseEntity.badRequest().body("거절 사유를 입력해주세요.");
	    }
	
	    Long adminId = memberMapper.getMemberId(authentication.getName());
	
	    if (adminId == null) {
	        return ResponseEntity.badRequest().body("관리자 정보를 찾을 수 없습니다.");
	    }
	
	    dto.setAdmin_id(adminId);
	
	    try {
	        int result = service.processPmChange(dto);

	        if (result == 1) {
	            return ResponseEntity.ok("PM 변경 요청 처리 완료");
	        }

	        return ResponseEntity.badRequest().body("처리 실패");

	    } catch (IllegalArgumentException | IllegalStateException e) {
	        return ResponseEntity.badRequest().body(e.getMessage());
	    }
	}

}
