package com.tk.app.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.DailyReportDto;
import com.tk.app.domain.dto.GrowthExpLogDto;
import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.mapper.GrowthTreeMapper;
import com.tk.app.security.LoginMemberUtil;

@Service
public class GrowthTreeService {
	
	private final GrowthTreeMapper growthTreeMapper;
	private final TaskService taskService;
	
	public GrowthTreeService(
			GrowthTreeMapper growthTreeMapper, 
			TaskService taskService) {
		
		this.growthTreeMapper = growthTreeMapper;
		this.taskService = taskService;
	}
	
    // 공통 로직 분리 시작 =============================================================================
	
	// 성장나무 잠금처리
	// 여러 활동의 보상이 동시에 들어와도 누적 경험치가 덮어써지지 않게하기 위함
	private GrowthTreeDto lockGrowthTree(long workspaceId) {
		GrowthTreeDto growthTree = growthTreeMapper.findGrowthTreeForUpdate(workspaceId);
		if(growthTree == null) {
			throw new IllegalStateException("워크의 성장나무 정보를 찾을 수 없습니다.");
		}
		
		return growthTree;
	}
	
	// 경험치 지급 이력 저장 및 조회
	private void saveGrowthExpLog(GrowthExpLogDto expLog) {
		// 경험치 지급 이력 저장 및 결과 확인
		int logInsertCount = growthTreeMapper.insertGrowthExpLog(expLog);
		if(logInsertCount != 1) {
			throw new IllegalStateException("경험치 지급 이력을 저장할 수 없습니다.");
		}
		
	}
	
	// 성장 단계 선택
	private GrowthLevelDto findGrowthLevelByExp(long newTotalExp) {
		// 새 누적 경험치에 해당하는 성장 단계 찾기
		List<GrowthLevelDto> growthLevels = growthTreeMapper.findGrowthLevels();
		GrowthLevelDto selectedLevel = null;
		
		// 기존 레벨과 성장레벨 순회하면서 비교
		for(GrowthLevelDto level : growthLevels) {
			if(level.getMinExp() <= newTotalExp) {
				// MaxExp 가 null 인 단계도 포함
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
		
		return selectedLevel;
	}
	
	private void saveGrowthTree(GrowthTreeDto growthTree, String errorMessage) {
		// 계산한 경험치와 단계를 DB에 저장
		int treeUpdateCount = growthTreeMapper.updateGrowthTree(growthTree);
		if(treeUpdateCount != 1) {
			throw new IllegalStateException(errorMessage);
		}
	}
	
	// 경험치 계산
	private long calculateTotalExpAfterRevoke(long currentTotalExp, long totalRevokeExp) {
		// 실제 지급 이력의 경험치를 회수
		long newTotalExp = currentTotalExp - totalRevokeExp;
		if(newTotalExp < 0) {
			newTotalExp = 0;
		}
		
		return newTotalExp;
	}
	
	// 검증이 끝난 뒤 지급 처리
	private GrowthLevelDto grantGrowthExp(GrowthTreeDto growthTree, GrowthExpLogDto expLog) {
		// 경험치 보관함
		long previousTotalExp = growthTree.getTotalExp();
		saveGrowthExpLog(expLog);
		
		// 지급 후 누적 경험치 계산
		// 현재 경험치 + 지급량
		long newTotalExp = growthTree.getTotalExp() + expLog.getExp();
		growthTree.setTotalExp(newTotalExp);
		
		// 새 누적 경험치에 해당하는 성장 단계 찾기
		GrowthLevelDto selectedLevel = findGrowthLevelByExp(newTotalExp);
		
		boolean levelUp = previousTotalExp < selectedLevel.getMinExp();
		
		growthTree.setGrowthLevelId(selectedLevel.getGrowthLevelId());
		
		// 계산된 경험치와 성장 단계 DB 에 저장
		saveGrowthTree(growthTree, "성장나무 경험치와 단계를 저장할 수 없습니다.");
		return levelUp ? selectedLevel : null;
	}
	
	// 경험치 회수
	private void revokeGrowthExp(GrowthTreeDto growthTree, long totalRevokeExp) {
		// 차감 후 경험치 계산하고 최소 0 적용하기
		// 현재 경험치가 5이고 회수 대상이 10이라면 결과는 0으로 만든다.
		long newTotalExp = calculateTotalExpAfterRevoke(growthTree.getTotalExp(), totalRevokeExp);
		growthTree.setTotalExp(newTotalExp);
		// 새 누적 경험치에 해당하는 성장 단계 찾기
		GrowthLevelDto selectedLevel = findGrowthLevelByExp(newTotalExp);
		
		growthTree.setGrowthLevelId(selectedLevel.getGrowthLevelId());
		
		// 계산한 경험치와 단계를 DB에 저장
		saveGrowthTree(growthTree, "회수 후 성장나무 경험치와 단계를 저장할 수 없습니다.");
	}
	
	// 로그인 조회와 NULL 검사, 로그인 여부를 확인하고 SessionInfo 를 반환
	private SessionInfo requireLoginMember() {
	    SessionInfo loginMember = LoginMemberUtil.getSessionInfo();
	    if (loginMember == null) {
	        throw new IllegalStateException("로그인이 필요합니다.");
	    }

	    return loginMember;
	}
	
	// 로그인 회원 검증 = 회원 번호 일치 검사
	private void validateLoginMember(long memberId) {
		// 로그인 회원 검증
		SessionInfo loginMember = requireLoginMember();
		
		if(memberId != loginMember.getMember_id()) {
			throw new IllegalStateException("활동 회원과 로그인 회원이 일치하지 않습니다.");
		}
	}
	
    private boolean isPmApprovalContext(
            Map<String, Object> context, String errorMessage) {
        if (context == null) {
            throw new IllegalStateException(errorMessage);
        }

        int reviewerRole = ((Number) context.get("reviewerRole")).intValue();
        return reviewerRole == 70;
    }
    
    // 공통 로직 분리 끝 =============================================================================
	
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
	
	// 성장나무 페이지 최근활동기록
	public List<GrowthExpLogDto> findRecentGrowthExpLogs(long workspaceId) {
		return growthTreeMapper.findRecentGrowthExpLogs(workspaceId);
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
	public GrowthLevelDto grantNoticeCreateExp(
			long workspaceId, long memberId, long noticeId) throws Exception {
		
		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
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
			return null;
		}
		return grantGrowthExp(growthTree, expLog);
	} 
	
	// 미회수 지급이력 조회 및 회수 시간 기록
	@Transactional(rollbackFor = Exception.class)
	public long revokeNoticeCreateExp(long workspaceId, long noticeId) throws Exception {
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// revokeGrowthExp : 조회 조건을 담은 객체
		GrowthExpLogDto revokeGrowthExp = new GrowthExpLogDto();
		revokeGrowthExp.setWorkspaceId(workspaceId);
		revokeGrowthExp.setNoticeId(noticeId);
		revokeGrowthExp.setActionType("NOTICE_CREATE");
		
		// revoke : 미회수 지급 이력
		List<GrowthExpLogDto> revoke = growthTreeMapper.findUnrevokedNoticeCreateExpLogs(revokeGrowthExp);
		if(revoke.isEmpty()) {
			return 0L;
		}
		
		// 회수할 경험치 합계 계산
		long totalRevokeExp = 0;
		for(GrowthExpLogDto exp : revoke) {
			totalRevokeExp += exp.getExp();
			// 각 이력에 회수 시간을 기록하고 저장 결과 확인
			int revokeCount = growthTreeMapper.markNoticeCreateExpLogRevoked(exp);
			if(revokeCount != 1) {
				throw new IllegalStateException("경험치 회수 시간을 기록할 수 없습니다.");
			}
		}
		// 회수 전후 누적 경험치 차이로 실제 차감량 계산
		long previousTotalExp = growthTree.getTotalExp();
		revokeGrowthExp(growthTree, totalRevokeExp);
		return previousTotalExp - growthTree.getTotalExp();
	}
	
	// 회원 초대 시 경험치 +5
	@Transactional(rollbackFor = Exception.class)
	public void grantInviteAcceptExp(
			long workspaceId, long memberId, String inviteKey) throws Exception {
		
		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// 지급 이력 조회 및 저장
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setInviteKey(inviteKey);
		expLog.setExp(5);
		expLog.setActionType("INVITE_ACCEPT");
		
		// 같은 워크 회원의 초대 보상 지급 이력
		int existingCount = growthTreeMapper.countInviteAcceptExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		
		// 탈퇴한 회원은 지급하지 않는다.
		int acceptedInviteCount = growthTreeMapper.countAcceptedInviteMember(expLog);
		if(acceptedInviteCount != 1) {
			throw new IllegalStateException("해당 초대의 수락 및 미탈퇴한 워크 회원만 등록이 가능합니다.");
		}
		grantGrowthExp(growthTree, expLog);
	}
	
	// 성장 경험치 지급 메서드 틀 추가
	@Transactional(rollbackFor = Exception.class)
	public void grantSubTaskCreateExp(
			long workspaceId, long memberId, long subTaskId) {
		
		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// 지급 이력 조회 및 저장
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setSubTaskId(subTaskId);
		expLog.setExp(10);
		expLog.setActionType("SUBTASK_CREATE");
		
		// 같은 하위 업무에 이미 지급한 이력이 있으면 종료
		int existingCount = growthTreeMapper.countSubTaskCreateExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		// 서비스에서 소속 조회를 호출하는 검사
		int subTaskCount = growthTreeMapper.countSubTaskInWorkspace(expLog);
		if(subTaskCount != 1) {
			throw new IllegalStateException("하위업무가 해당 워크에 속하지 않거나 유효하지 않습니다.");
		}
		grantGrowthExp(growthTree, expLog);
	}
	
	// 회수 서비스 메서드 호출
	@Transactional(rollbackFor = Exception.class)
	public void revokeSubTaskCreateExp(
			long workspaceId, long subTaskId) {
		
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// revokeGrowthExp : 조회 조건을 담은 객체
		GrowthExpLogDto revokeGrowthExp = new GrowthExpLogDto();
		revokeGrowthExp.setWorkspaceId(workspaceId);
		revokeGrowthExp.setSubTaskId(subTaskId);
		revokeGrowthExp.setActionType("SUBTASK_CREATE");
		
		// revoke : 미회수 지급 이력
		List<GrowthExpLogDto> revoke = growthTreeMapper.findUnrevokedSubTaskCreateExpLogs(revokeGrowthExp);
		if(revoke.isEmpty()) {
			return;
		}
		
		// 회수할 경험치 합계 계산
		long totalRevokeExp = 0;
		for(GrowthExpLogDto exp : revoke) {
			totalRevokeExp += exp.getExp();
			// 각 이력에 회수 시간을 기록하고 저장 결과 확인
			int revokeCount = growthTreeMapper.markSubTaskCreateExpLogRevoked(exp);
			if(revokeCount != 1) {
				throw new IllegalStateException("경험치 회수 시간을 기록할 수 없습니다.");
			}
		}
		revokeGrowthExp(growthTree, totalRevokeExp);	
	}
	
	// 일일 보고서 등록 시 경험치 +10
	@Transactional(rollbackFor = Exception.class)
	public void grantDailyReportCreateExp(
			long workspaceId, long memberId, long dailyReportId) {
		
		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// 지급 이력 조회 및 저장
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setDailyReportId(dailyReportId);
		expLog.setExp(10);
		expLog.setActionType("DAILY_REPORT_CREATE");
		
		// 같은 일일 보고서에 이미 지급한 이력이 있으면 종료
		int existingCount = growthTreeMapper.countDailyReportCreateExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		
		int dailyReportCount = growthTreeMapper.countDailyReportInWorkspace(expLog);
		if(dailyReportCount != 1) {
			throw new IllegalStateException("일일 보고서가 해당 워크에 속하지 않거나 유효하지 않습니다.");
		}
		grantGrowthExp(growthTree, expLog);
	}
	
	// 일일 보고서 삭제 시 경험치 회수
	@Transactional(rollbackFor = Exception.class)
	public void revokeDailyReportCreateExp(long workspaceId, long dailyReportId) {
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// revokeGrowthExp : 조회 조건을 담은 객체
		GrowthExpLogDto revokeGrowthExp = new GrowthExpLogDto();
		revokeGrowthExp.setWorkspaceId(workspaceId);
		revokeGrowthExp.setDailyReportId(dailyReportId);
		revokeGrowthExp.setActionType("DAILY_REPORT_CREATE");
		
		// revoke : 미회수 지급 이력
		List<GrowthExpLogDto> revoke = growthTreeMapper.findUnrevokedDailyReportCreateExpLogs(revokeGrowthExp);
		if(revoke.isEmpty()) {
			return;
		}
		
		// 회수할 경험치 합계 계산
		long totalRevokeExp = 0;
		for(GrowthExpLogDto exp : revoke) {
			totalRevokeExp += exp.getExp();
			// 각 이력에 회수 시간을 기록하고 저장 결과 확인
			int revokeCount = growthTreeMapper.markDailyReportCreateExpLogRevoked(exp);
			if(revokeCount != 1) {
				throw new IllegalStateException("경험치 회수 시간을 기록할 수 없습니다.");
			}
		}
		revokeGrowthExp(growthTree, totalRevokeExp);
	}
	// 일일보고서 등록 검증 메소드
	public Map<String, Object> findDailyReportCreateContext(
			Long subTaskId, Long projectId, String domainKey) {
		
		SessionInfo loginMember = requireLoginMember();
		
		Map<String, Object> params = new HashMap<>();
		params.put("subTaskId", subTaskId);
		params.put("projectId", projectId);
		params.put("domainKey", domainKey);
		params.put("memberId", loginMember.getMember_id());
		
		Map<String, Object> context;
		context = growthTreeMapper.selectDailyReportCreateContext(params);
		// 작성 권한이 없거나 유효하지 않은 대상
		if(context == null) {
			throw new IllegalStateException("작성 권한이 없거나 유효하지 않은 대상입니다.");
		}
		
		context.put("memberId", loginMember.getMember_id());
		return context;
	}
	
	// 삭제 전에 일일보고서의 실제 소속과 삭제 권한 확인
	public Map<String, Object> findDailyReportDeleteContext(
			Long dailyReportId, Long projectId, String domainKey) {
		
		SessionInfo loginMember = requireLoginMember();
		
		Map<String, Object> params = new HashMap<>();
		params.put("dailyReportId", dailyReportId);
		params.put("projectId", projectId);
		params.put("domainKey", domainKey);
		params.put("memberId", loginMember.getMember_id());
		
		Map<String, Object> context;
		context = growthTreeMapper.selectDailyReportDeleteContext(params);
		// 작성 권한이 없거나 유효하지 않은 대상
		if(context == null) {
			throw new IllegalStateException("삭제 권한이 없거나 삭제할 수 없는 보고서입니다.");
		}
		
		context.put("memberId", loginMember.getMember_id());
		return context;
	}
	
	// 업무 보고서 등록 시 경험치 +20 지급
	@Transactional(rollbackFor = Exception.class)
	public void grantTeamReportCreateExp(
			long workspaceId, long memberId, long teamReportId) {
		
		validateLoginMember(memberId);

		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// 지급 이력 조회 및 저장
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setTeamReportId(teamReportId);
		expLog.setExp(20);
		expLog.setActionType("TEAM_REPORT_CREATE");
		
		// 같은 업무 보고서에 이미 지급한 이력이 있으면 종료
		int existingCount = growthTreeMapper.countTeamReportCreateExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		
		int teamReportCount = growthTreeMapper.countTeamReportInWorkspace(expLog);
		if(teamReportCount != 1) {
			throw new IllegalStateException("업무 보고서가 해당 워크에 속하지 않거나 유효하지 않습니다.");
		}
		grantGrowthExp(growthTree, expLog);
	}
	
	// 업무 보고서 경험치 회수
	@Transactional(rollbackFor = Exception.class)
	public void revokeTeamReportCreateExp(
			long workspaceId, long teamReportId) {
		
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// revokeGrowthExp : 조회 조건을 담은 객체
		GrowthExpLogDto revokeGrowthExp = new GrowthExpLogDto();
		revokeGrowthExp.setWorkspaceId(workspaceId);
		revokeGrowthExp.setTeamReportId(teamReportId);
		revokeGrowthExp.setActionType("TEAM_REPORT_CREATE");
		
		// revoke : 미회수 지급 이력
		List<GrowthExpLogDto> revoke = growthTreeMapper.findUnrevokedTeamReportCreateExpLogs(revokeGrowthExp);
		if(revoke.isEmpty()) {
			return;
		}
		
		// 회수할 경험치 합계 계산
		long totalRevokeExp = 0;
		for(GrowthExpLogDto exp : revoke) {
			totalRevokeExp += exp.getExp();
			// 각 이력에 회수 시간을 기록하고 저장 결과 확인
			int revokeCount = growthTreeMapper.markTeamReportCreateExpLogRevoked(exp);
			if(revokeCount != 1) {
				throw new IllegalStateException("경험치 회수 시간을 기록할 수 없습니다.");
			}
		}
		revokeGrowthExp(growthTree, totalRevokeExp);
	}
	
	// 삭제 전에 업무보고서의 실제 소속과 삭제 권한 확인
	@Transactional(rollbackFor = Exception.class)
		public Map<String, Object> selectTeamReportDeleteContext(
				Long teamReportId, Long projectId, String domainKey) {
		
		SessionInfo loginMember = requireLoginMember();
		
		Map<String, Object> params = new HashMap<>();
		params.put("teamReportId", teamReportId);
		params.put("projectId", projectId);
		params.put("domainKey", domainKey);
		params.put("memberId", loginMember.getMember_id());
		
		Long lockedTeamReportId = growthTreeMapper.lockTeamReportForDelete(params);
		if(lockedTeamReportId == null) {
			throw new IllegalStateException("보고서가 없거나 이미 삭제되었습니다.");
		}
		
		Map<String, Object> context;
		context = growthTreeMapper.selectTeamReportDeleteContext(params);
		// 작성 권한이 없거나 유효하지 않은 대상
		if(context == null) {
			throw new IllegalStateException("삭제 권한이 없거나 삭제할 수 없는 보고서입니다.");
		}
		
		context.put("memberId", loginMember.getMember_id());
		return context;
	}
	
	// 업무 보고서 등록 소속 조회
	public Map<String, Object> findTeamReportCreateContext(
			Long teamReportId, Long projectId, String domainKey) {
		
		SessionInfo loginMember = requireLoginMember();
		
		// 조회 조건
		Map<String, Object> params = new HashMap<>();
		params.put("teamReportId", teamReportId);
		params.put("projectId", projectId);
		params.put("domainKey", domainKey);
		params.put("memberId", loginMember.getMember_id());
		
		Map<String, Object> context = growthTreeMapper.selectTeamReportCreateContext(params);
		if(context == null) {
			throw new IllegalStateException("보고서가 없거나 작성자, 소속 정보가 일치하지 않습니다.");
		}
		
		context.put("memberId", loginMember.getMember_id());
		return context;
	}
	
	// 업무 보고서 승인 보상 +20
	@Transactional(rollbackFor = Exception.class)
	public void grantTeamReportApproveExp(
			long teamReportId, long reviewId, long memberId) {
		
		// 로그인 회원 검증
		validateLoginMember(memberId);
		
		// 승인 정보 조회와 조회 실패 처리
		// params : 조회 조건
		Map<String, Object> params = new HashMap<>();
		params.put("teamReportId", teamReportId);
		params.put("reviewId", reviewId);
		params.put("memberId", memberId);
		
		// context : 실제 워크 번호와 검토자 역할
		Map<String, Object> context = growthTreeMapper.selectTeamReportApproveContext(params);
        if (!isPmApprovalContext(
                context, "승인된 업무 보고서 정보를 확인할 수 없습니다.")) {
            return;
        }
		long workspaceId = ((Number) context.get("workspaceId")).longValue();
		// 잠금 처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setTeamReportId(teamReportId);
		expLog.setExp(20);
		expLog.setActionType("TEAM_REPORT_APPROVE");
		
		// 같은 업무 보고서에 아직 회수되지 않은 승인 보상이 있으면 지급하지 않는다
		int existingCount = growthTreeMapper.countTeamReportApproveExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		grantGrowthExp(growthTree, expLog);
	}
	
	// 경험치 등록 및 지급
	@Transactional(rollbackFor = Exception.class)
	public DailyReportDto createDailyReportWithExp(
			DailyReportDto dto, Long projectId, String domainKey) {
		
		// 경험치 등록 전 검증 호출
		Map<String, Object> context = findDailyReportCreateContext(dto.getSubTaskId(), projectId, domainKey);
		long projectMemberId = ((Number) context.get("projectMemberId")).longValue();
		dto.setProjectMemberId(projectMemberId);
		
		// 일일 보고서 확인
		DailyReportDto savedReport = taskService.createDailyReport(dto);
		if(savedReport == null || savedReport.getDailyReportId() == null) {
			throw new IllegalStateException("일일 보고서 등록 결과를 확인할 수 없습니다.");
		}
		
		// 경험치 지급 호출
		long workspaceId = ((Number) context.get("workspaceId")).longValue();
		long memberId = ((Number) context.get("memberId")).longValue();
		grantDailyReportCreateExp(workspaceId, memberId, savedReport.getDailyReportId());
		
		return savedReport;
	}
	
	// 기존 권한, 소속 검증, 삭제, 경험치 회수 재사용
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteDailyReportWithExp(
			Long dailyReportId, Long projectId, String domainKey) {

		if (dailyReportId == null || projectId == null
				|| domainKey == null || domainKey.isBlank()) {
			throw new IllegalArgumentException("삭제 요청 정보를 확인할 수 없습니다.");
		}

		Map<String, Object> context = findDailyReportDeleteContext(
				dailyReportId, projectId, domainKey);

		long workspaceId = ((Number) context.get("workspaceId")).longValue();

		boolean deleted = taskService.deleteDailyReport(dailyReportId);

		if (!deleted) {
			return false;
		}

		revokeDailyReportCreateExp(workspaceId, dailyReportId);
		return true;
	}
	
	// 일일 보고서 승인 시 +10
	@Transactional(rollbackFor = Exception.class)
	public void grantDailyReportApproveExp(
			long dailyReportId, long reviewId, long memberId) {
		
		validateLoginMember(memberId);
		
		Map<String, Object> params = new HashMap<>();
		params.put("dailyReportId", dailyReportId);
		params.put("reviewId", reviewId);
		params.put("memberId", memberId);
		
		Map<String, Object> context = growthTreeMapper.selectDailyReportApproveContext(params);
        if (!isPmApprovalContext(
                context, "승인된 일일 보고서 정보를 확인할 수 없습니다.")) {
            return;
        }
		
		long workspaceId = ((Number) context.get("workspaceId")).longValue();
		
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setDailyReportId(dailyReportId);
		expLog.setExp(10);
		expLog.setActionType("DAILY_REPORT_APPROVE");
		
		// 같은 업무 보고서에 이미 지급한 이력이 있으면 종료
		int existingCount = growthTreeMapper.countDailyReportApproveExpLog(expLog);
		if(existingCount > 0) {
			return;
		}
		grantGrowthExp(growthTree, expLog);
	}
	
	
	// PM 이 업무 보고서 반려 시 경험치 실제 지급된 검토 보상 회수 시간 기록
	@Transactional(rollbackFor = Exception.class)
	public void revokeTeamReportApproveExp(
			long workspaceId, long teamReportId) {
		// 나무 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		
		GrowthExpLogDto condition = new GrowthExpLogDto();
		condition.setWorkspaceId(workspaceId);
		condition.setTeamReportId(teamReportId);
		
		List<GrowthExpLogDto> expLogs = 
				growthTreeMapper.findUnrevokedTeamReportApproveExpLogs(condition);
		
		// 기록이 비어있으면
		if(expLogs.isEmpty()) {
			return;
		}
		
		// 총 회수 경험치
		long totalRevokeExp = 0;
		// 미회수 검토 보상 이력 하나씩 처리
		for(GrowthExpLogDto expLog : expLogs) {
			int revokeCount = 
					growthTreeMapper.markTeamReportApproveExpLogRevoked(expLog);
			// 회수 시간을 기록한 행수가 1이 아니면 실패
			if(revokeCount != 1) {
				throw new IllegalStateException("검토 보상 회수 시간을 기록할 수 없습니다.");
			}
			// 해당 이력의 실제 지급액을 총 회수량에 합산
			totalRevokeExp += expLog.getExp();
		}
		revokeGrowthExp(growthTree, totalRevokeExp);
	}
	
	// 프로젝트 보고서 PM 이 최종 승인 시 +30
	@Transactional(rollbackFor = Exception.class)
	public void selectProjectReportApproveContext(
			long reviewId, long projectReportId, long memberId,
			int reportCategory, int reviewStatus) {
		
		// 로그인 회원 검증
		validateLoginMember(memberId);
		
		// 지급 대상 확인
		if(reportCategory != 1 || reviewStatus != 1) {
			return;
		}
		
		Map<String, Object> params = new HashMap<>();
		params.put("reviewId", reviewId);
		params.put("projectReportId", projectReportId);
		params.put("memberId", memberId);
		
		// 추후 공통 로직 분리
		Map<String, Object> context = growthTreeMapper.selectProjectReportApproveContext(params);
        if (!isPmApprovalContext(
                context, "승인된 프로젝트 보고서 정보를 확인할 수 없습니다.")) {
            return;
        }
		
        long workspaceId = ((Number) context.get("workspaceId")).longValue();
		// 잠금나무로 전달
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(memberId);
		expLog.setProjectReportId(projectReportId);
		expLog.setExp(30);
		expLog.setActionType("PROJECT_REPORT_APPROVE");
		
		// 미회수 승인 보상이 있으면 지급 전에 리턴
		int existingCount = growthTreeMapper.countProjectReportApproveExpLog(expLog);
		if (existingCount > 0) {
		    return;
		}
		
		// 이력 저장, 누적 경험치 및 레벨 갱신
		grantGrowthExp(growthTree, expLog);

	}
	
	// PM 프로젝트 보고서 결재 정보를 받아 승인 보상 로직으로 연결하는 메서드
	@Transactional(rollbackFor = Exception.class)
	public void grantProjectReportApproveExp(Map<String, Object> reviewContext) {
		
		long reviewId = ((Number) reviewContext.get("reviewId")).longValue();
		long projectReportId = ((Number) reviewContext.get("reportId")).longValue();
		long memberId = ((Number) reviewContext.get("memberId")).longValue();
		int reportCategory = ((Number) reviewContext.get("reportCategory")).intValue();
		int reviewStatus = ((Number) reviewContext.get("reviewStatus")).intValue();
	
		// 기존 지급 메서드에 값 전달
		selectProjectReportApproveContext(reviewId, projectReportId, memberId, reportCategory, reviewStatus);
	}
	
	// 프로젝트 보고서 승인 보상 회수
	@Transactional(rollbackFor = Exception.class)
	public void revokeProjectReportApproveExp(
			long workspaceId, long projectReportId) {
		
		// 나무 잠금
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		GrowthExpLogDto condition = new GrowthExpLogDto();
		condition.setWorkspaceId(workspaceId);
		condition.setProjectReportId(projectReportId);
		
		// 프로젝트 보고서의 미회수 승인 보상 이력 조회
		List<GrowthExpLogDto> expLogs = 
				growthTreeMapper.findUnrevokedProjectReportApproveExpLogs(condition);
		// 경험치 목록이 비어있으면 리턴
		if(expLogs.isEmpty()) {
			return;
		}
		// 회수할 경험치 합계
		long totalRevokeExp = 0;
		// 미회수 승인 보상 이력 순회하면서 하나씩 처리
		for(GrowthExpLogDto expLog : expLogs) {
			// 해당 지급 이력에 회수 시간 기록
			int revokeCount = 
					growthTreeMapper.markProjectReportApproveExpLogRevoked(expLog);
		
			// 1행이 갱신되지 않으면 예외 처리
			if(revokeCount != 1) {
				throw new IllegalStateException("검토 보상 회수 시간을 기록할 수 없습니다.");
			}
		
			// 회수 시간 기록에 성공한 이력 실제 지급액 더해주기
			totalRevokeExp += expLog.getExp();
		}
		// 합산한 경험치 회수, 최소 0 적용, 성장 단계 재계산 및 저장
		revokeGrowthExp(growthTree, totalRevokeExp);
	}
	
	// 프로젝트 진행률 완료된 것 카운트
	@Transactional(rollbackFor = Exception.class)
	public void grantProjectCompleteExp(long projectId) {
		
		// 유효한 프로젝트인지 확인
		if(projectId <= 0) {
			throw new IllegalStateException("프로젝트를 찾을 수 없습니다.");
		}
		
		// 로그인 회원 검증
		SessionInfo loginMember = requireLoginMember();
		
		// 프로젝트 확인, context 에 담기
		Map<String, Object> context = 
				growthTreeMapper.findProjectCompleteContext(projectId);
		
		if(context == null) {
			throw new IllegalStateException("프로젝트 정보를 찾을 수 없습니다.");
		}
		
		// 진행률 값을 꺼내 숫자 변수에 담기
		Number progress = (Number) context.get("progress");
		
		if(progress == null) {
			throw new IllegalStateException("프로젝트 진행률 정보를 찾을 수 없습니다.");
		}
		
		// 진행률이 100% 가 아니면
		if(progress.doubleValue() != 100.0) {
			return;
		}
		
		// 이 프로젝트에 보상을 지급했다면 다시 지급하지 않고 종료한다.
		if(growthTreeMapper.countProjectCompleteExpLog(projectId) > 0) {
			return;
		}
		
		// 워크 번호 형변환
		long workspaceId = ((Number) context.get("workspaceId")).longValue();
		
		// 워크의 성장나무를 조회하고 잠금처리
		GrowthTreeDto growthTree = lockGrowthTree(workspaceId);
		// 지급 이력 조회 및 저장
		GrowthExpLogDto expLog = new GrowthExpLogDto();
		expLog.setWorkspaceId(workspaceId);
		expLog.setMemberId(loginMember.getMember_id());
		expLog.setProjectId(projectId);
		expLog.setExp(50);
		expLog.setActionType("PROJECT_COMPLETE");
		
		grantGrowthExp(growthTree, expLog);
	}
}
