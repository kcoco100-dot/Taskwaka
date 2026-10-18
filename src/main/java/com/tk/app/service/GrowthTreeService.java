package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.GrowthExpLogDto;
import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;
import com.tk.app.mapper.GrowthTreeMapper;

@Service
public class GrowthTreeService {
	
	private final GrowthTreeMapper growthTreeMapper;
	
	public GrowthTreeService(GrowthTreeMapper growthTreeMapper) {
		this.growthTreeMapper = growthTreeMapper;
	}
	
	// 성장나무 목록 조회
	public List<GrowthLevelDto> findGrowthLevels() {
		
		return growthTreeMapper.findGrowthLevels();
		
	}
	
	// 성장 상태 조회 메서드
	public GrowthTreeDto findGrowthTree(long workspaceId) {
		
		// 성장나무가 없는 워크에서 기본값 처리
		GrowthTreeDto growthTree = growthTreeMapper.findGrowthTree(workspaceId);
		if(growthTree == null) {
			// 새 DTO 만들기
			growthTree = new GrowthTreeDto();
			growthTree.setWorkspaceId(workspaceId);
			growthTree.setTotalExp(0);
		
		
		// 단계 목록 조회
		List<GrowthLevelDto> growthList = findGrowthLevels();
		// 0 EXP 가 포함되는 단계 찾기
		for(GrowthLevelDto growthLists : growthList) {
			if(growthLists.getMinExp() <= 0 
					&& (growthLists.getMaxExp() == null || growthLists.getMaxExp() >= 0)) {
				growthTree.setGrowthLevelId(growthLists.getGrowthLevelId());
				growthTree.setLevelNo(growthLists.getLevelNo());
				growthTree.setLevelName(growthLists.getLevelName());
				growthTree.setTreeImagePath(growthLists.getTreeImagePath());
				
				break;
			}
		}		
	}
		
		return growthTree;
	}
	
	// 성장나무 초기 저장
	public int growthTreeCreate(long workspaceId) throws Exception {
		int count = growthTreeMapper.growthTreeCreate(workspaceId);
		
		if(count != 1) {
			throw new IllegalStateException("성장나무 초기 생성에 실패했습니다.");
		} 
		
		return count;
	}
	
	// 공지 등록 시 + 10 경험치 등록
	@Transactional(rollbackFor = Exception.class)
	public void grantNoticeCreateExp(
			long workspaceId, long memberId, long noticeId) throws Exception {
		
		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = growthTreeMapper.findGrowthTreeForUpdate(workspaceId);
		if(growthTree == null) {
			throw new IllegalStateException("워크의 성장나무 정보를 찾을 수 없습니다.");
		}
		
		// 지급 이력 조회 및 저장
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setNoticeId(noticeId);
		expLog.setExp(10);
		expLog.setActionType("NOTICE_CREATE");
		
		// 같은 공지에 이미 지급한 이력이 있으면 종료
		int existingCount = growthTreeMapper.countNoticeCreateExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		
		// 경험치 지급 이력 저장 및 결과 확인
		int logInsertCount = growthTreeMapper.insertGrowthExpLog(expLog);
		if(logInsertCount != 1) {
			throw new IllegalStateException("경험치 지급 이력을 저장할 수 없습니다.");
		}
		
		// 지급 후 누적 경험치 계산
		// 현재 경험치 + 지급량
		long newTotalExp = growthTree.getTotalExp() + expLog.getExp();
		growthTree.setTotalExp(newTotalExp);
		
		// 새 누적 경험치에 해당하는 성장 단계 찾기
		List<GrowthLevelDto> growthLevels = growthTreeMapper.findGrowthLevels();
		GrowthLevelDto selectedLevel = null;
		
		// 기존 레벨과 성장레벨 순회하면서 비교
		for(GrowthLevelDto level : growthLevels) {
			// MinExp 가 newTotalExp 이하
			if(level.getMinExp() <= newTotalExp) {
				// MaxExp 가 newTotalExp 이상
				if(level.getMaxExp() == null || level.getMaxExp() >= newTotalExp) {
					// selectedLevel 에 level 해당 레벨 담기
					selectedLevel = level;
					break;
				}
			}
		}
		
		if(selectedLevel == null) {
			throw new IllegalStateException("경험치에 맞는 성장 단계를 찾을 수 없습니다.");
		}
		
		growthTree.setGrowthLevelId(selectedLevel.getGrowthLevelId());
	
		// 계산된 경험치와 성장 단계 DB 에 저장
		int treeUpdateCount = growthTreeMapper.updateGrowthTree(growthTree);
		if(treeUpdateCount != 1) {
			throw new IllegalStateException("성장나무 경험치와 단계를 저장할 수 없습니다.");
		}
	} 
}
