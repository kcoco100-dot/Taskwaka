package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.GrowthExpLogDto;
import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;

@Mapper
public interface GrowthTreeMapper {
	
	// 성장나무 단계 화면
	public List<GrowthLevelDto> findGrowthLevels();
	
	// 성장나무 화면
	public GrowthTreeDto findGrowthTree(long workspaceId);
	
	// 성장나무 초기 저장
	public int growthTreeCreate(long workspaceId);
	
	// 경험치 지급 이력
	public int insertGrowthExpLog(GrowthExpLogDto dto);
	
	// 같은 공지의 등록 보상을 이미 지급했는지 확인
	public int countNoticeCreateExpLog(GrowthExpLogDto dto);
	
	// 동시에 들어온 경험치 요청을 순서대로 처리
	public GrowthTreeDto findGrowthTreeForUpdate(long workspaceId);

	// 변경된 누적 경험치와 성장 단계 저장
	public int updateGrowthTree(GrowthTreeDto dto);
}
