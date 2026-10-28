package com.tk.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;
import com.tk.app.domain.dto.HomeDashboardDataDto;
import com.tk.app.domain.dto.HomePersonalScheduleDto;
import com.tk.app.domain.dto.MyTaskDto;
import com.tk.app.domain.dto.ParticipatingProjectDto;
import com.tk.app.service.GrowthTreeService;
import com.tk.app.service.HomePersonalScheduleService;
import com.tk.app.service.MyTaskService;
import com.tk.app.service.ParticipatingProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class HomeDashboardController {
	
	private final ParticipatingProjectService participatingProjectService;
	private final MyTaskService myTaskService;
	private final GrowthTreeService growthTreeService;
	private final HomePersonalScheduleService homePersonalScheduleService;
	
	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}
	
	// 기존 home 화면, 내 업무 목록 조회
	@GetMapping({ "", "/dashboard" })
	public String dashboard(
			@RequestAttribute("workspace") WorkspaceDto worksapce, 
			Model model) {

		List<MyTaskDto> myTasks = myTaskService.findTasksList(
				worksapce.getWorkspaceId(), worksapce.getMemberId());

		HomeDashboardDataDto dashboardData = myTaskService.calculateHomeDashboardData(myTasks);
		
		model.addAttribute("myTasks", myTasks);
		model.addAttribute("remainingCount", dashboardData.getRemainingCount());
		model.addAttribute("view", dashboardData.getView());
		model.addAttribute("working", dashboardData.getWorking());
		model.addAttribute("preview", dashboardData.getPreview());
		
		// 오늘을 기준으로 보여줌
		model.addAttribute("today", dashboardData.getToday());			
		model.addAttribute("weekDates", dashboardData.getWeekDates());
		model.addAttribute("todayTasks", dashboardData.getTodayTasks());
		model.addAttribute("monthlyCompletedCount", dashboardData.getMonthlyCompletedCount());

		// 참여 프로젝트 화면에서 사용중인 service 재사용
		// 회원이 참여하는 프로젝트 목록 가져오기
		List<ParticipatingProjectDto> participatingProjects = participatingProjectService
				.findParticipatingProjects(worksapce.getWorkspaceId(), worksapce.getMemberId());

		model.addAttribute("participatingProjects", participatingProjects);

		GrowthTreeDto growthTree = growthTreeService.findGrowthTree(worksapce.getWorkspaceId());
		
		// 목표 경험치를 찾기 위해 성장단계 목록 조회
		List<GrowthLevelDto> growthLevels = growthTreeService.findGrowthLevels();
		GrowthLevelDto nextGrowthLevel = null;
		for(GrowthLevelDto level : growthLevels) {
			if(level.getLevelNo() > growthTree.getLevelNo()) {
				nextGrowthLevel = level;
				break;
			}
		}
		
		// 진행률 = 누적 경험치 / 목표 경험치 * 100
		int growthPercent = 0;
		// 다음 단계가 없을 경우
		if(nextGrowthLevel == null) {
			growthPercent = 100;
			// 다음 단계가 있을때 진행률 계산 후 growthPercent 에 저장
		} else if(nextGrowthLevel.getMinExp() > 0){
			// 누적 경험치 / 목표 경험치 * 100
			// Math.max : 결과는 0보다 작아지지 않게 함
			// Math.min : 계산값은 100을 초과하지 않는다.
			growthPercent = (int) Math.max(0, Math.min(100, 
					growthTree.getTotalExp() * 100.0 / nextGrowthLevel.getMinExp()));
		} else {
			growthPercent = 0;
		}
		
		model.addAttribute("growthTree", growthTree);
		model.addAttribute("nextGrowthLevel", nextGrowthLevel);
		model.addAttribute("growthPercent", growthPercent);
		
		return "main/dashboard";
	}
	
	// 개인 일정 캘린더에 등록
	public String personalSchedule(
			@RequestAttribute("workspace") WorkspaceDto workspace, 
			Model model) {
		
		// 개인 일정 목록을 조회
		model.addAttribute("personalSchedules", 
				homePersonalScheduleService.findPersonalSchedules(
						workspace.getWorkspaceId(), workspace.getMemberId()));
		
		// 캘린더의 참여 프로젝트 표시를 위한 목록 조회
		model.addAttribute("participatingProjects",
				participatingProjectService.findParticipatingProjects(
						workspace.getWorkspaceId(), workspace.getMemberId()));
		
		// 캘린더에 함께 표시할 기존 담당 업무 목록
		model.addAttribute("myTasks",
				myTaskService.findTasksList(
						workspace.getWorkspaceId(), workspace.getMemberId()));
		
		return "main/home";
	}
	
	// 개인 일정 등록요청
	@PostMapping("/personal-schedules")
	@ResponseBody
	public int insertPersonalSchedule(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@RequestBody HomePersonalScheduleDto dto) {
		
		return homePersonalScheduleService.insertPersonalSchedule(
				workspace.getWorkspaceId(), workspace.getMemberId(), dto);
	}
	
	// 캘린더 일정
	@GetMapping("/personal-schedules")
	@ResponseBody
	public List<HomePersonalScheduleDto> findPersonalSchedules(
			@RequestAttribute("workspace") WorkspaceDto workspace) {
		
		return homePersonalScheduleService.findPersonalSchedules(
				workspace.getWorkspaceId(), workspace.getMemberId());
	}
}
