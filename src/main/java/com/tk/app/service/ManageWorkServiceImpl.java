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

	// 검색어 정리
	private String searchKeyword(String kwd) {
		String result = "";
		result = "%" + (kwd == null ? "" : kwd.trim().toLowerCase()) + "%";
		return result;
	}
	
	// 워크 직위 조회
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

	// 워크 팀원 건수 조회
	@Override
	public int dataCount(Long workspaceId, Long actorId, String kwd) throws Exception {
		int result = 0;
		try {
			if (workspaceId == null || actorId == null) {
				throw new IllegalArgumentException("워크스페이스 정보가 없습니다.");
			}
			result = Math.toIntExact(repository.dataCount(workspaceId, searchKeyword(kwd)));
		} catch (Exception e) {
			log.info("dataCount : ", e);
			throw e;
		}
		return result;
	}

	// 워크 팀원 목록 조회
	@Override
	public List<ManageWorkDto> listWorkMember(Long workspaceId, Long actorId, String kwd, int current_page, int size) throws Exception {
		List<ManageWorkDto> result = new ArrayList<>();
		try {
			if (workspaceId == null || actorId == null) {
				throw new IllegalArgumentException("워크스페이스 정보가 없습니다.");
			}
			if (size < 1) {
				throw new IllegalArgumentException("페이지 크기가 올바르지 않습니다.");
			}
			result = repository.listWorkMember(
				workspaceId,
				searchKeyword(kwd),
				PageRequest.of(Math.max(1, current_page) - 1, size)
			);
		} catch (Exception e) {
			log.info("listWorkMember : ", e);
			throw e;
		}
		return result;
	}

	// 워크 직위 변경
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void updateWorkRole(Long workspaceId, Long actorId, Long workspaceMemberId, int role) throws Exception {
		int result = 0;
		try {
			if (workspaceId == null || actorId == null) {
				throw new IllegalArgumentException("워크스페이스 정보가 없습니다.");
			}
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

	// 워크 사장 직위 변경
	@Transactional(rollbackFor = Exception.class)
	@Override
	public int requestChange(Long workspaceId, Long memberId, Long newMemberId, String reason) throws Exception {
		int result = 0;
		try {
			
			if (workspaceId == null || workspaceId <= 0 || memberId == null || memberId <= 0 || newMemberId == null || newMemberId <= 0) {
	            throw new IllegalArgumentException("워크 또는 회원 정보가 올바르지 않습니다.");
	        }
			
	        if (memberId.equals(newMemberId)) {
	            throw new IllegalArgumentException("자기 자신을 교체 대상으로 선택할 수 없습니다.");
	        }
	        
	        if (reason == null || reason.isBlank()) {
	            throw new IllegalArgumentException("교체 신청 사유를 입력해주세요.");
	        }
	        
	        reason = reason.trim();
	        
	        Long lockedWorkspaceId = repository.lockWorkspaceForChange(workspaceId);
	        
	        if (lockedWorkspaceId == null) {
	            throw new IllegalArgumentException("워크스페이스를 찾을 수 없습니다.");
	        }
	        
	        if (repository.countValidChangeMembers(workspaceId, memberId, newMemberId) != 1) {
	            throw new IllegalArgumentException("현재 사장만 같은 워크의 활성 과장을 대상으로 신청할 수 있습니다.");
	        }
	        
	        if (repository.countpendingRequests(workspaceId) > 0) {
	            throw new IllegalStateException("이미 승인 대기 중인 사장 교체 신청이 있습니다.");
	        }

	        result = repository.insertChangeRequest(workspaceId, memberId, newMemberId, reason);
	        if (result != 1) {
	            throw new IllegalStateException("사장 교체 신청을 저장하지 못했습니다.");
	        }
	        
		} catch (Exception e) {
			log.info("requestChange : ", e);
			
			throw e;
		}
		
		
		return result;
	}
}
