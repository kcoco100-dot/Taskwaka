package com.tk.app.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tk.app.admin.domain.dto.AdminWorkspaceDto;
import com.tk.app.admin.service.AdminWorkspaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/workspaces")
public class AdminWorkspaceApiController {

	private final AdminWorkspaceService service;

	@GetMapping
	public List<AdminWorkspaceDto> findAllWorkspaces() {
		return service.findAllWorkspaces();
	}

	@PostMapping("/status")
	public ResponseEntity<String> updateWorkspaceStatus(
			@RequestBody AdminWorkspaceDto dto) {

		if (!"Y".equals(dto.getActive()) && !"N".equals(dto.getActive())) {
			return ResponseEntity.badRequest().body("잘못된 상태 값입니다.");
		}

		if ("N".equals(dto.getActive()) && (dto.getReason() == null || dto.getReason().trim().isEmpty())) {
			return ResponseEntity.badRequest().body("정지 사유를 입력해주세요.");
		}

		if (dto.getWorkspace_id() <= 0) {
			return ResponseEntity.badRequest().body("잘못된 워크 ID입니다.");
		}

		int result = service.updateWorkspaceStatus(dto);

		if (result == 0) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok("워크 상태 변경 완료");
	}

}