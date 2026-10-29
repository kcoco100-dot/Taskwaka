package com.tk.app.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.common.PaginateUtil;
import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.domain.dto.ParticipatingProjectDto;
import com.tk.app.domain.dto.TaskDto;
import com.tk.app.service.MeetingMinutesService;
import com.tk.app.service.ParticipatingProjectService;
import com.tk.app.service.TaskService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class HomeMeetingMinutesController {
	
	private final ParticipatingProjectService participatingProjectService;
	private final MeetingMinutesService meetingMinutesService;
	private final PaginateUtil paginateUtil;
	private final TaskService taskService;
	
	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}
	
	// home - meetings 목록 페이지
		@GetMapping("/meetings")
		public String meetings(@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam(value = "kwd", defaultValue = "") String kwd,
				@RequestParam(value = "page", defaultValue = "1") int page, 
				HttpServletRequest request, 
				Model model) {

			int size = 10;

			// 검색 조건에 맞는 건수 & 페이지 수
			int dataCount = meetingMinutesService.countMeetingMinutes(
					workspace.getWorkspaceId(), workspace.getMemberId(),
					kwd);

			int totalPage = paginateUtil.pageCount(dataCount, size);

			// 만약 페이지가 1보다 작거나 전체 페이지가 0이면 1로 맞춰주기
			if (page < 1 || totalPage == 0) {
				page = 1;
			} else if (page > totalPage) {
				page = totalPage;
			}

			int offset = (page - 1) * size;

			// 회의록 목록 조회
			List<MeetingMinutesDto> meetingMinutes = 
					meetingMinutesService.findMeetingMinutes(workspace.getWorkspaceId(),
					workspace.getMemberId(), kwd, offset, size);

			// 현재 워크 목록 주소 유지
			// getRequestURI : 현재 도메인이 포함된 목록 경로 가져오기
			String listUrl = request.getRequestURI();

			// 페이지 이동 시 검색어 유지하기
			if (!kwd.isEmpty()) {
				listUrl += "?kwd=" + URLEncoder.encode(kwd, StandardCharsets.UTF_8);
			}

			// 페이지 번호와 이동 링크가 들어있는 HTML을 만듦
			// htmlEscape : 특수 문자 변환 (& -> &amp; 로 변경)
			String paging = paginateUtil.paging(page, totalPage, HtmlUtils.htmlEscape(listUrl));

			model.addAttribute("paging", paging);
			model.addAttribute("kwd", kwd);
			model.addAttribute("meetingMinutes", meetingMinutes);

			return "home/meetings";
		}
		
		// home - meetingsDetail 페이지
		@GetMapping("/meetingsDetail")
		public String meetingsDetail(
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("meetingMinuteId") long meetingMinuteId, 
				Model model,
				HttpServletResponse response) {

			try {
				MeetingMinutesDto meeting = 
						meetingMinutesService.findMeetingDetailMinutes(workspace.getWorkspaceId(),
						workspace.getMemberId(), meetingMinuteId);

				if (meeting == null) {
					response.setStatus(HttpStatus.NOT_FOUND.value());
					model.addAttribute("title", "자료를 찾을 수 없습니다.");
					model.addAttribute("message", "회의록을 찾을 수 없습니다.");
					return "error/error2";
				}
				
				MeetingMinutesDto prevMeeting = meetingMinutesService.findByPrevMeeting(workspace.getWorkspaceId(), workspace.getMemberId(), meetingMinuteId);
				MeetingMinutesDto nextMeeting = meetingMinutesService.findByNextMeeting(workspace.getWorkspaceId(), workspace.getMemberId(), meetingMinuteId);

				model.addAttribute("meeting", meeting);
				model.addAttribute("prevMeeting", prevMeeting);
				model.addAttribute("nextMeeting", nextMeeting);

				return "home/meetingsDetail";
				
				// DataAccessException : DB 처리 실패 오류
			} catch (DataAccessException e) {
				log.error("meetingsDetail - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
				}
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("회의록 상세 조회 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
					
				}
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}
		
		// 회의록 등록
		@GetMapping("/meetingsCreate")
		public String meetingCreate(
				@RequestAttribute("workspace") WorkspaceDto workspace,
				Model model) {
			
			model.addAttribute("meetingCreateDate",
					LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
			model.addAttribute("participatingProjects", 
					participatingProjectService.findParticipatingProjects(
							workspace.getWorkspaceId(), workspace.getMemberId()));
			
			return "home/meetingsCreate";
		}
		
		// 회의록에서 하위 업무 조회
		// @ResponseBody : 조회 결과를 JSON 으로 반환
		@GetMapping("/meetingTasks")
		@ResponseBody
		public List<TaskDto> meetingTasks(
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("projectId") long projectId) {
			
			List<ParticipatingProjectDto> projects =
					participatingProjectService.findParticipatingProjects(
							workspace.getWorkspaceId(), workspace.getMemberId());
			
			boolean participating = false;
			
			for(ParticipatingProjectDto project : projects) {
				if(project.getProjectId() == projectId) {
					participating = true;
					break;
				}
			}
			
			if(!participating) {
				// HTTP 응답 상태 코드 
				// FORBIDDEN : 403 에러 
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참여중인 프로젝트가 아닙니다.");
			}
			
			return taskService.getTaskList(projectId);
		}
		
		// 회의록 등록저장
		@PostMapping("meetingsIn")
		public String insertMeetingMinutes(
				@PathVariable("domainKey") String domainKey,
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("projectId") long projectId,
				@RequestParam("subTaskId") long subTaskId,
				@RequestParam("title") String title,
				@RequestParam("content") String content,
				HttpServletResponse response,
				Model model) {

			try {

				if (title.isBlank() || title.getBytes(StandardCharsets.UTF_8).length > 200 || content.isBlank()) {
					response.sendError(400, "제목은 UTF-8 기준 200바이트 이내이며, 제목과 내용은 공백만 입력할 수 없습니다.");
					return null;
				}

				int insertCount = 
						meetingMinutesService.insertMeetingMinutes(
								workspace.getWorkspaceId(), projectId, subTaskId, 
								workspace.getMemberId(), title, content);

				// 등록 검사 결과 및 정상 반환
				if(insertCount != 1) {
					response.sendError(403, "선택한 프로젝트와 하위업무에는 회의록을 등록할 수 없습니다.");
					return null;
				}

				return "redirect:/" + domainKey + "/home/meetings";
			} catch (DataAccessException e) {
				log.error("insertMeetingMinutes - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("회의록 등록 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
					
				}
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}

		// HOME 회의록 수정 화면 조회
		@GetMapping("/meetingsEdit")
		public String meetingsEdit(
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("meetingMinuteId") long meetingMinuteId, 
				HttpServletResponse response, 
				Model model) {

			try {
				MeetingMinutesDto meeting = 
						meetingMinutesService.findMeetingDetailMinutes(workspace.getWorkspaceId(),
						workspace.getMemberId(), meetingMinuteId);

				// 회의록이 없으면
				if (meeting == null) {
					response.sendError(404, "회의록을 찾을 수 없습니다.");
					return null;
				}

				// 회의록 수정 권한이 없으면
				if (meeting.getCanModify() == 0) {
					response.sendError(403, "회의록 수정 권한이 없습니다. 관리자에게 문의하세요.");
					return null;
				}

				model.addAttribute("meeting", meeting);

				return "home/meetingsEdit";
				
			} catch (DataAccessException e) {
				log.error("meetingsEdit - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
					
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("회의록 수정 화면 조회 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
					
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}

		// 회의록 수정 저장
		@PostMapping("meetingsEdit")
		public String meetingsEdit(
				@PathVariable("domainKey") String domainKey,
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("meetingMinuteId") long meetingMinuteId, @RequestParam("title") String title,
				@RequestParam("content") String content, 
				HttpServletResponse response, Model model) {

			try {
				MeetingMinutesDto meeting = meetingMinutesService.findMeetingDetailMinutes(workspace.getWorkspaceId(),
						workspace.getMemberId(), meetingMinuteId);

				if (meeting == null) {
					response.sendError(404, "회의록을 찾을 수 없습니다.");
					return null;
				}

				if (meeting.getCanModify() == 0) {
					response.sendError(403, "회의록 수정 권한이 없습니다. 관리자에게 문의하세요.");
					return null;
				}

				if (title.isBlank() || title.length() > 200 || content.isBlank()) {
					meeting.setTitle(title);
					meeting.setContent(content);

					model.addAttribute("meeting", meeting);
					model.addAttribute("errorMessage", "제목은 200자 이내로 입력하고, 내용은 빈칸없이 입력해 주세요.");

					return "home/meetingsEdit";
				}

				int updateCount = meetingMinutesService.meetingsEdit(
						workspace.getWorkspaceId(), workspace.getMemberId(),
						meetingMinuteId, title, content);

				if (updateCount == 0) {
					response.sendError(404, "수정할 회의록이 없습니다.");
					return null;
				}

				return "redirect:/" + domainKey + "/home/meetingsDetail?meetingMinuteId=" + meetingMinuteId;
			} catch (DataAccessException e) {
				
				log.error("meetingsEdit - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
					
				}
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("회의록 수정 저장 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}

		// 회의록 삭제
		@PostMapping("/meetingsDelete")
		public String meetingsDelete(@PathVariable("domainKey") String domainKey,
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("meetingMinuteId") long meetingMinuteId, 
				HttpServletResponse response, Model model) {

			try {
				// meetingsDelete 는 int 를 반환하기 때문에 dto 에 담을 수 없음!!!
				// 삭제가 아닌 삭제 조회로 해야 함
				MeetingMinutesDto meeting = 
						meetingMinutesService.findMeetingDetailMinutes(workspace.getWorkspaceId(),
						workspace.getMemberId(), meetingMinuteId);

				if (meeting == null) {
					response.sendError(404, "회의록을 찾을 수 없습니다.");
					return null;
				}

				if (meeting.getCanModify() == 0) {
					response.sendError(403, "삭제할 권한이 없습니다. 관리자에게 문의하세요.");
					return null;
				}

				int deleteCount = meetingMinutesService.meetingsDelete(
						workspace.getWorkspaceId(), workspace.getMemberId(),
						meetingMinuteId);

				if (deleteCount == 0) {
					response.sendError(404, "삭제할 회의록이 없습니다.");
					return null;
				}

				return "redirect:/" + domainKey + "/home/meetings";
				
			} catch (DataAccessException e) {
				log.error("meetingsDelete - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
				}
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("회의록 삭제 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}

}
