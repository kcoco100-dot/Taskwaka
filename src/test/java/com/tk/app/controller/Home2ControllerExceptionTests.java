package com.tk.app.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.MeetingMinutesDto;
import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.domain.dto.NoticeFileDto;
import com.tk.app.service.MeetingMinutesService;
import com.tk.app.service.MyTaskService;
import com.tk.app.service.NoticeService;
import com.tk.app.service.ParticipatingProjectService;
import com.tk.app.service.TaskService;

class Home2ControllerExceptionTests {

	private NoticeService noticeService;
	private MeetingMinutesService meetingMinutesService;
	private StorageService storageService;
	private WorkspaceDto workspace;
	private MeetingMinutesDto meeting;
	private MockMvc mvc;

	@BeforeEach
	void setUp() throws Exception {
		noticeService = mock(NoticeService.class);
		meetingMinutesService = mock(MeetingMinutesService.class);
		storageService = mock(StorageService.class);
		Home2Controller controller = new Home2Controller(
				mock(ParticipatingProjectService.class), noticeService, meetingMinutesService,
				new PaginateUtil(), mock(MyTaskService.class), mock(TaskService.class), storageService);
		mvc = MockMvcBuilders.standaloneSetup(controller).build();
		workspace = new WorkspaceDto();
		workspace.setWorkspaceId(1L);
		workspace.setMemberId(2L);
		meeting = new MeetingMinutesDto();
		meeting.setCanModify(1);
		when(meetingMinutesService.findMeetingDetailMinutes(1L, 2L, 10L)).thenReturn(meeting);
		when(meetingMinutesService.insertMeetingMinutes(1L, 3L, 4L, 2L, "제목", "내용")).thenReturn(1);
		when(meetingMinutesService.meetingsEdit(1L, 2L, 10L, "제목", "내용")).thenReturn(1);
		when(meetingMinutesService.meetingsDelete(1L, 2L, 10L)).thenReturn(1);
		when(noticeService.findWorkspaceDetailNotice(1L, 10L)).thenReturn(new NoticeDto());
		when(noticeService.findWorkspaceFiles(1L, 10L)).thenReturn(List.of());
		when(noticeService.createWorkspaceNotice(1L, 2L, "제목", "내용", null)).thenReturn(1);
		when(noticeService.noticeEdit(1L, 10L, "제목", "내용", null, null)).thenReturn(1);
		when(noticeService.deleteNotice(1L, 10L)).thenReturn(1);
		NoticeFileDto file = new NoticeFileDto();
		file.setFilePath("files");
		file.setStoredName("stored.txt");
		file.setOriginalName("original.txt");
		when(noticeService.findWorkspaceFile(1L, 10L, 20L)).thenReturn(file);
	}

	static Object[][] successfulPages() {
		return new Object[][] {
				{"meetingDetail", "home/meetingsDetail", 200},
				{"meetingCreate", "redirect:/sample/home/meetings", 302},
				{"meetingEditForm", "home/meetingsEdit", 200},
				{"meetingEditSave", "redirect:/sample/home/meetingsDetail?meetingMinuteId=10", 302},
				{"meetingDelete", "redirect:/sample/home/meetings", 302},
				{"noticeDetail", "notice/noticeDetail", 200},
				{"noticeCreate", "redirect:/sample/home/notice", 302},
				{"noticeEditForm", "notice/noticeEdit", 200},
				{"noticeEditSave", "redirect:/sample/home/noticeDetail?noticeId=10", 302},
				{"noticeDelete", "redirect:/sample/home/notice", 302}
		};
	}

	@ParameterizedTest
	@MethodSource("successfulPages")
	void keepsSuccessfulViewsAndRedirects(String action, String expectedView, int expectedStatus) throws Exception {
		mvc.perform(request(action))
				.andExpect(status().is(expectedStatus))
				.andExpect(view().name(expectedView));
	}

	@ParameterizedTest
	@MethodSource("successfulPages")
	void handlesServiceFailuresWithExistingErrorView(String action, String expectedView, int expectedStatus) throws Exception {
		IllegalStateException failure = new IllegalStateException("내부 오류 상세");
		when(meetingMinutesService.findMeetingDetailMinutes(anyLong(), anyLong(), anyLong())).thenThrow(failure);
		when(meetingMinutesService.insertMeetingMinutes(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString())).thenThrow(failure);
		when(noticeService.findWorkspaceDetailNotice(anyLong(), anyLong())).thenThrow(failure);
		when(noticeService.createWorkspaceNotice(anyLong(), anyLong(), anyString(), anyString(), any())).thenThrow(new Exception("파일 저장 실패"));
		when(noticeService.noticeEdit(anyLong(), anyLong(), anyString(), anyString(), any(), any())).thenThrow(new Exception("파일 수정 실패"));
		when(noticeService.deleteNotice(anyLong(), anyLong())).thenThrow(failure);

		mvc.perform(request(action))
				.andExpect(status().isInternalServerError())
				.andExpect(view().name("error/error2"))
				.andExpect(model().attribute("title", "시스템 오류."))
				.andExpect(model().attribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>"))
				.andExpect(model().attribute("domainKey", "sample"))
				.andExpect(model().attribute("workspace", workspace));
	}

	@ParameterizedTest
	@ValueSource(strings = {"meetingEditForm", "meetingEditSave", "meetingDelete"})
	void keepsMeetingPermissionChecks(String action) throws Exception {
		meeting.setCanModify(0);
		mvc.perform(request(action)).andExpect(status().isForbidden());
		verify(meetingMinutesService, never()).meetingsEdit(anyLong(), anyLong(), anyLong(), anyString(), anyString());
		verify(meetingMinutesService, never()).meetingsDelete(anyLong(), anyLong(), anyLong());
	}

	@ParameterizedTest
	@ValueSource(strings = {"meetingEditForm", "meetingEditSave", "meetingDelete"})
	void keepsMissingMeetingChecks(String action) throws Exception {
		when(meetingMinutesService.findMeetingDetailMinutes(1L, 2L, 10L)).thenReturn(null);
		mvc.perform(request(action)).andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = {"noticeEditSave", "noticeDelete", "meetingEditSave", "meetingDelete"})
	void keepsZeroAffectedRowChecks(String action) throws Exception {
		when(noticeService.noticeEdit(1L, 10L, "제목", "내용", null, null)).thenReturn(0);
		when(noticeService.deleteNotice(1L, 10L)).thenReturn(0);
		when(meetingMinutesService.meetingsEdit(1L, 2L, 10L, "제목", "내용")).thenReturn(0);
		when(meetingMinutesService.meetingsDelete(1L, 2L, 10L)).thenReturn(0);
		mvc.perform(request(action)).andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = {"noticeCreate", "noticeEditSave"})
	void preservesInvalidNoticeInput(String action) throws Exception {
		String expectedView = action.equals("noticeCreate") ? "notice/noticeCreate" : "notice/noticeEdit";
		mvc.perform(request(action, ""))
				.andExpect(status().isOk())
				.andExpect(view().name(expectedView))
				.andExpect(model().attributeExists("errorMessage"))
				.andExpect(result -> assertEquals("", ((NoticeDto) result.getModelAndView().getModel().get("notice")).getTitle()));
		verify(noticeService, never()).createWorkspaceNotice(anyLong(), anyLong(), anyString(), anyString(), any());
		verify(noticeService, never()).noticeEdit(anyLong(), anyLong(), anyString(), anyString(), any(), any());
	}

	@Test
	void rejectsInvalidMeetingCreation() throws Exception {
		mvc.perform(request("meetingCreate", ""))
				.andExpect(status().isBadRequest());
		verify(meetingMinutesService, never()).insertMeetingMinutes(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyString());
	}

	@Test
	void keepsSuccessfulDownload() throws Exception {
		doReturn(ResponseEntity.ok("download")).when(storageService).downloadFile("files", "stored.txt", "original.txt");
		mvc.perform(downloadRequest()).andExpect(status().isOk()).andExpect(content().string("download"));
	}

	@Test
	void handlesMissingDownloadMetadata() throws Exception {
		when(noticeService.findWorkspaceFile(1L, 10L, 20L)).thenThrow(new Exception("파일 내부 정보"));
		mvc.perform(downloadRequest()).andExpect(status().isInternalServerError())
				.andExpect(content().encoding("UTF-8"))
				.andExpect(content().string("첨부파일을 다운로드할 수 없습니다."));
		verify(storageService, never()).downloadFile(anyString(), anyString(), anyString());
	}

	@Test
	void handlesStorageDownloadFailure() throws Exception {
		when(storageService.downloadFile("files", "stored.txt", "original.txt")).thenThrow(new IllegalStateException("저장소 오류"));
		mvc.perform(downloadRequest()).andExpect(status().isInternalServerError())
				.andExpect(content().encoding("UTF-8"))
				.andExpect(content().string("첨부파일을 다운로드할 수 없습니다."));
	}

	private MockHttpServletRequestBuilder downloadRequest() {
		return get("/sample/home/noticeFileDownload").requestAttr("workspace", workspace)
				.param("noticeId", "10").param("noticeFileId", "20");
	}

	private MockHttpServletRequestBuilder request(String action) {
		return request(action, "제목");
	}

	private MockHttpServletRequestBuilder request(String action, String title) {
		MockHttpServletRequestBuilder builder = switch (action) {
			case "meetingDetail" -> get("/sample/home/meetingsDetail");
			case "meetingCreate" -> post("/sample/home/meetingsIn");
			case "meetingEditForm" -> get("/sample/home/meetingsEdit");
			case "meetingEditSave" -> post("/sample/home/meetingsEdit");
			case "meetingDelete" -> post("/sample/home/meetingsDelete");
			case "noticeDetail" -> get("/sample/home/noticeDetail");
			case "noticeCreate" -> post("/sample/home/noticeCreate");
			case "noticeEditForm" -> get("/sample/home/noticeEdit");
			case "noticeEditSave" -> post("/sample/home/noticeEdit");
			case "noticeDelete" -> post("/sample/home/noticeDelete");
			default -> throw new IllegalArgumentException(action);
		};
		return builder.requestAttr("workspace", workspace).param("meetingMinuteId", "10")
				.param("noticeId", "10").param("projectId", "3").param("subTaskId", "4")
				.param("title", title).param("content", "내용");
	}
}
