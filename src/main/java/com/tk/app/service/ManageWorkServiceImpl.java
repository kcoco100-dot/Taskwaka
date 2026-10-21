package com.tk.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageWorkDto;
import com.tk.app.repository.ManageWorkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageWorkServiceImpl implements ManageWorkService {
	private final ManageWorkRepository repository;
	
	// 사장 권한 확인
	private void checkBossRole(Long workspaceId, Long actorId) throws Exception {
		try {
			if (workspaceId == null || actorId == null) {
				throw new IllegalArgumentException("워크스페이스 또는 회원 정보가 없습니다.");
			}
			Integer role = findWorkRole(workspaceId, actorId);
			if (role == null || role != 70) {
				throw new SecurityException("사장만 워크스페이스 직위를 관리할 수 있습니다.");
			}
		} catch (Exception e) {
			log.info("checkBossRole : ", e);
			throw e;
		}
	}
	
	// 검색어 처리
	private String searchKeyword(String keyword) {
		String result = "";
		result = "%" + (keyword == null ? "" : keyword.trim().toLowerCase()) + "%";
		return result;
	}
	
	@Override
	public int findWorkRole(Long workspaceId, Long memberId) throws Exception {
		int result = 0;  
		try {
			result = repository.findWorkRole(workspaceId, memberId);
		} catch (Exception e) {
			log.info("findWorkRole : ", e);
			
			throw e;
		}
		return result;
	}

	// 워크 팀원 수
	@Override
	public int dataCount(Long workspaceId, Long actorId, String keyword) throws Exception {
		int result = 0;
		try {
			checkBossRole(workspaceId, actorId);
			result = Math.toIntExact(repository.dataCount(workspaceId, searchKeyword(keyword)));
		} catch (Exception e) {
			log.info("dataCount : ", e);
			throw e;
		}
		return result;
	}

	// 워크 팀원 목록
	@Override
	public List<ManageWorkDto> listWorkMember(Long workspaceId, Long actorId, String keyword, int current_page, int size) throws Exception {
		List<ManageWorkDto> result = new ArrayList<>();
		try {
			checkBossRole(workspaceId, actorId);
			if (size < 1) {
				throw new IllegalArgumentException("페이지 크기가 올바르지 않습니다.");
			}
			result = repository.listWorkMember(
				workspaceId,
				searchKeyword(keyword),
				PageRequest.of(Math.max(1, current_page) - 1, size)
			);
		} catch (Exception e) {
			log.info("listWorkMember : ", e);
			throw e;
		}
		return result;
	}
	
	// 워크 직위 변경
	@Transactional
	@Override
	public void updateWorkRole(Long workspaceId, Long actorId, Long workspaceMemberId, int role) throws Exception {
		int result = 0;
		try {
			checkBossRole(workspaceId, actorId);
			if (workspaceMemberId == null) {
				throw new IllegalArgumentException("팀원을 선택해주세요.");
			}
			if (role != 50 && role != 30 && role != 10) {
				throw new IllegalArgumentException("과장, 대리, 사원 직위만 부여할 수 있습니다.");
			}
			result = repository.updateWorkRole(workspaceId, actorId, workspaceMemberId, role);
			if (result != 1) {
				throw new IllegalArgumentException("직위를 변경할 수 없는 팀원입니다.");
			}
		} catch (Exception e) {
			log.info("updateWorkRole : ", e);
			throw e;
		}
	}
}