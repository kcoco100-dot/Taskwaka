package com.tk.app.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.domain.dto.GrowthLevelDto;
import com.tk.app.domain.dto.GrowthTreeDto;
import com.tk.app.domain.dto.HomeDashboardDataDto;
import com.tk.app.domain.dto.HomePersonalScheduleDto;
import com.tk.app.domain.dto.HomeTodayScheduleDto;
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
		// log.info("대시보드 내 업무 조회: workspaceId={}, memberId={}, taskCount={}", worksapce.getWorkspaceId(), worksapce.getMemberId(), myTasks.size());

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
		
		// 오늘 프로젝트 일정
		List<ParticipatingProjectDto> todayProjects = new ArrayList<>();
		for(ParticipatingProjectDto projects : participatingProjects) {
			if (projects.getStartAt() == null || projects.getStartAt().isBlank()
					|| projects.getEndAt() == null || projects.getEndAt().isBlank()) {
				continue;
			}
			
			LocalDate start = LocalDate.parse(projects.getStartAt());
			LocalDate end = LocalDate.parse(projects.getEndAt());
			
			if(!dashboardData.getToday().isBefore(start) && !dashboardData.getToday().isAfter(end)) {
				todayProjects.add(projects);
			}
		}
		model.addAttribute("todayProjects", todayProjects);

		// 오늘의 업무와 프로젝트를 같은 형태의 목록으로 합치기
		List<HomeTodayScheduleDto> todaySchedules = new ArrayList<>();

		for (MyTaskDto task : dashboardData.getTodayTasks()) {
			
			HomeTodayScheduleDto schedule = new HomeTodayScheduleDto();
			
			schedule.setScheduleType("TASK");
			schedule.setTitle(task.getTitle());
			schedule.setDescription(task.getSubject());
			schedule.setEndAt(LocalDate.parse(task.getEndAt()));
			schedule.setStatus(task.getStatus());

			todaySchedules.add(schedule);
		}

		for (ParticipatingProjectDto project : todayProjects) {
			HomeTodayScheduleDto schedule = new HomeTodayScheduleDto();
			schedule.setScheduleType("PROJECT");
			schedule.setTitle(project.getSubject());
			schedule.setDescription(
					project.getStartAt() + " ~ " + project.getEndAt());
			schedule.setEndAt(LocalDate.parse(project.getEndAt()));

			todaySchedules.add(schedule);
		}

		// 업무와 프로젝트를 합쳐 마감일이 가까운 순서로 정렬
		todaySchedules.sort(Comparator.comparing(HomeTodayScheduleDto::getEndAt));

		// 정렬한 목록에서 최대 5개만 화면에 전달
		model.addAttribute("todaySchedules",
				todaySchedules.subList(0, Math.min(5, todaySchedules.size())));
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
	
	// HOME 개인 일정 수정
	@PutMapping("/personal-schedules/{scheduleId}")
	@ResponseBody
	public int updatePersonalSchedule(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@PathVariable("scheduleId") long scheduleId,
			@RequestBody HomePersonalScheduleDto dto) {

		return homePersonalScheduleService.updatePersonalSchedule(
				workspace.getWorkspaceId(), workspace.getMemberId(),
				scheduleId, dto);
	}
	
	// HOME 개인 일정 삭제
	@DeleteMapping("/personal-schedules/{scheduleId}")
	@ResponseBody
	public int deletePersonalSchedule(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@PathVariable("scheduleId") long scheduleId) {

		return homePersonalScheduleService.deletePersonalSchedule(
				workspace.getWorkspaceId(), workspace.getMemberId(), scheduleId);
	}

	// 캘린더 일정
	@GetMapping("/personal-schedules")
	@ResponseBody
	public List<HomePersonalScheduleDto> findPersonalSchedules(
			@RequestAttribute("workspace") WorkspaceDto workspace) {
		
		return homePersonalScheduleService.findPersonalSchedules(
				workspace.getWorkspaceId(), workspace.getMemberId());
	}
	
	// 참여 프로젝트 조회 
	@GetMapping("/participating-projects")
	@ResponseBody
	public List<ParticipatingProjectDto> findParticipatingProjects(
			@RequestAttribute("workspace") WorkspaceDto workspace) {
		
		return participatingProjectService.findParticipatingProjects(
				workspace.getWorkspaceId(), workspace.getMemberId());
	}
}
