package com.tk.app.controller;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.admin.domain.dto.WorkspaceDto;
import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.NoticeCreateResultDto;
import com.tk.app.domain.dto.NoticeDeleteResultDto;
import com.tk.app.domain.dto.NoticeDto;
import com.tk.app.domain.dto.NoticeFileDto;
import com.tk.app.exception.StorageException;
import com.tk.app.exception.StorageFileNotFoundException;
import com.tk.app.service.NoticeService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/home")
public class HomeNoticeController {
	
	private final NoticeService noticeService;
	private final StorageService storageService;
	private final PaginateUtil paginateUtil;
	private final MyUtil myUtil;
	
	@ModelAttribute("domainKey")
	public String setDomainKey(@PathVariable("domainKey") String domainKey) {

		return domainKey;
	}

	// 인터셉터가 조회한 워크 정보를 화면에서 ${workspace} 로 사용할 수 있게 전달
	@ModelAttribute("workspace")
	public WorkspaceDto setWorkspace(@RequestAttribute("workspace") WorkspaceDto workspace) {

		return workspace;
	}
	
	@GetMapping("/notice")
	public String notice(
			@RequestAttribute("workspace") WorkspaceDto workspace, 
			@RequestParam(name = "page", defaultValue = "1") String pageValue,
			@RequestParam(name = "schType", defaultValue = "all") String schType,
			@RequestParam(name = "kwd", defaultValue = "") String kwd,
			Model model) {

		int size = 10;
		int dataCount = 0;
		int totalPage = 0;
		int page;
		
		try {
			page = Integer.parseInt(pageValue.trim());
		} catch (NumberFormatException e) {
			page = 1;
		}
		
		kwd = myUtil.isEmpty(kwd) ? "" : kwd;

		dataCount = noticeService.countWorkspaceNotice(
				workspace.getWorkspaceId(), schType, kwd);
		if (dataCount != 0) {
			totalPage = paginateUtil.pageCount(dataCount, size);
		}

		// 다른 사람이 자료를 삭제하여 전체 페이지수가 변화 된 경우
		page = Math.max(1, Math.min(page, totalPage));

		// 리스트에 출력할 데이터를 가져오기
		int offset = (page - 1) * size;
		if(offset < 0) offset = 0;

		// 글 리스트
		List<NoticeDto> list = noticeService.findWorkspaceNotice(
				workspace.getWorkspaceId(), offset, size, schType, kwd);

		String query = "";
		// getRequestURI : 페이지 번호를 눌러도 현재 워크의 공지 목록으로 이동하도록 주소를 만든다
		String listUrl = RequestUtils.getCurrentRequest().getRequestURI();
		
		if (! kwd.isBlank()) { // if(kwd.length() != 0) {
			query = "schType=" + myUtil.encodeUrl(schType) + "&kwd=" + myUtil.encodeUrl(kwd);
			
			listUrl += "?" + query;
		}

		String paging = paginateUtil.paging(page, totalPage, listUrl);

		model.addAttribute("notices", list);
		model.addAttribute("page", page);
		model.addAttribute("dataCount", dataCount);
		model.addAttribute("size", size);
		model.addAttribute("totalPage", totalPage);
		model.addAttribute("paging", paging);

		model.addAttribute("schType", schType);
		model.addAttribute("kwd", kwd);

		return "notice/notice";
	}
	
	// 공지사항 상세조회
	// HttpSession : 사용자의 요청 사이에 정보를 기억하는 곳
	@GetMapping("/noticeDetail")
	public String noticeDetail(
			@RequestAttribute("workspace") WorkspaceDto workspace,
			@RequestParam("noticeId") long noticeId, Model model,
			HttpSession session,
			HttpServletResponse response) {

		try {
			// 공지별 열람 기록을 구분할 문자열 변수 추가
			String noticeViewKey = "workspaceNoticeViewed:" + workspace.getWorkspaceId() + ":" + noticeId;
			// 상세 열람 조회에서 조회수 증가 여부
			NoticeDto notice = new NoticeDto();
			
			// 같은 세션에서 동시에 들어온 요청의 중복 증가 방지
			synchronized (session) {

			if(Long.valueOf(noticeId).equals(model.getAttribute("editedNoticeId")) 
					|| Boolean.TRUE.equals(session.getAttribute(noticeViewKey))) {
				notice = noticeService.findWorkspaceDetailNotice(workspace.getWorkspaceId(), noticeId);
			} else {
				notice = noticeService.readWorkspaceNotice(workspace.getWorkspaceId(), noticeId);
			}
			
			if (notice == null) {
				response.setStatus(HttpStatus.NOT_FOUND.value());
				model.addAttribute("title", "자료를 찾을 수 없습니다.");
				model.addAttribute("message", "공지를 찾을 수 없습니다.");
				return "error/error2";
			}
			
			// 조회에 성공한 공지를 세션에 기록
			session.setAttribute(noticeViewKey, Boolean.TRUE);
		}
			// 등록된 공지사항의 첨부파일 조회
			List<NoticeFileDto> noticeFile = 
					noticeService.findWorkspaceFiles(workspace.getWorkspaceId(), noticeId);

			// 이전글 다음글
			NoticeDto prevNotice = noticeService.findByPrevNotice(workspace.getWorkspaceId(), noticeId);
			NoticeDto nextNotice = noticeService.findByNextNotice(workspace.getWorkspaceId(), noticeId);
			
			model.addAttribute("notice", notice);
			model.addAttribute("noticeFile", noticeFile);
			model.addAttribute("prevNotice", prevNotice);
			model.addAttribute("nextNotice", nextNotice);
			
			return "notice/noticeDetail";
			
		} catch (DataAccessException e) {
			
			log.error("noticeDetail - DataAccessException", e);
			if (response.isCommitted()) {
				return null;
				
			}
			
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			model.addAttribute("title", "데이터 처리 오류.");
			model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
			
			return "error/error2";
			
		} catch (Exception e) {
			log.error("공지 상세 조회 중 오류가 발생했습니다.", e);
			if (response.isCommitted()) {
				return null;
				
			}
			
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			model.addAttribute("title", "시스템 오류.");
			model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
			
			return "error/error2";
		}
	}
	
		// 공지사항 등록
		// 공지를 새로 등록할 때 워크번호나 회원번호로 조회&저장할 일이 없음
		// binding = false : 서버에서 조회한 역할을 요청값으로 바인딩하지 않도록 지정
		@GetMapping("/noticeCreate")
		public String noticeCreate(
				@ModelAttribute(value = "manageWorkspaceRole", binding = false) int workspaceRole,
				Model model,
				HttpServletResponse response) {
			
			// 공지 등록은 PM과 DT만 허용
			if (workspaceRole != 70 && workspaceRole != 50) {
				
				response.setStatus(HttpStatus.FORBIDDEN.value());
				
				model.addAttribute("title", "접근 권한이 없습니다.");
				model.addAttribute("message", "공지 등록은 PM과 DT만 가능합니다.");
				
				return "error/error2";
			}

			// title, content 는 여기 DTO 에서 사용할 수 있음
			model.addAttribute("notice", new NoticeDto());

			return "notice/noticeCreate";
		}

		// 공지사항 등록 저장 및 파일첨부
		// RedirectAttributes : 리다이렉트 시 데이터를 숨겨 넘기는 방법
		@PostMapping("/noticeCreate")
		public String noticeCreate(@PathVariable("domainKey") String domainKey,
				@RequestAttribute("workspace") WorkspaceDto workspace, 
				@RequestParam("title") String title,
				@RequestParam("content") String content,
				@RequestParam(value = "files", required = false) List<MultipartFile> files, // required = false 는 첨부파일이 선택사항이기 때문
				RedirectAttributes redirectAttributes,
				HttpServletResponse response, Model model,
				@ModelAttribute(value = "manageWorkspaceRole", binding = false) int workspaceRole) {
			
			// 공지 등록은 PM과 DT만 허용
			if (workspaceRole != 70 && workspaceRole != 50) {
				
				response.setStatus(HttpStatus.FORBIDDEN.value());
				
				model.addAttribute("title", "접근 권한이 없습니다.");
				model.addAttribute("message", "공지 등록은 PM과 DT만 가능합니다.");
				
				return "error/error2";
			}


			try {
				if (title.isBlank() || title.length() > 200 || content.isBlank()) {
					NoticeDto notice = new NoticeDto();
					notice.setTitle(title);
					notice.setContent(content);

					model.addAttribute("notice", notice);
					model.addAttribute("errorMessage", "제목은 200자 이내로 입력하고, 내용은 빈칸없이 입력해 주세요.");

					return "notice/noticeCreate";
				}
				// 첨부파일 검증
				String errorMessage = noticeService.validateNoticeFiles(files);

				// 첨부 검사에서 오류 메시지가 있다면
				if(errorMessage != null) {
					// 화면에 돌려줄 dto 객체 만들기
					NoticeDto notice = new NoticeDto();
					notice.setTitle(title);
					notice.setContent(content);

					model.addAttribute("notice", notice);
					model.addAttribute("errorMessage", errorMessage);

					return "notice/noticeCreate";
				}

				NoticeCreateResultDto noticeResult = noticeService.createWorkspaceNotice(
						workspace.getWorkspaceId(), workspace.getMemberId(),
						title, content, files);

				// 0 이랑 비교하는게 아니라 != 1 로 비교함
				if (noticeResult.getInsertCount() != 1) {
					response.sendError(500, "공지를 등록할 수 없습니다.");
					return null;

				}
				// 경험치 등록 성공 후 목록 화면(모달창)에 전달할 메시지
				String growthExpMessage = "🌱 워크 성장나무 경험치가 +10 올랐습니다.";

				if (noticeResult.getLevelUpInfo() != null) {
				    growthExpMessage += " ✨나무가 "
				            + noticeResult.getLevelUpInfo().getLevelNo()
				            + "단계(" + noticeResult.getLevelUpInfo().getLevelName()
				            + ")로 성장했어요!✨";
				}

				redirectAttributes.addFlashAttribute("growthExpMessage", growthExpMessage);
				
				return "redirect:/" + domainKey + "/home/notice";
			} catch (DataAccessException e) {
				
				log.error("noticeCreate - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
					
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (StorageException e) {
				log.error("noticeCreate - StorageException", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "파일 처리 오류.");
				model.addAttribute("message", "첨부파일을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("공지 등록 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}

		// 공지사항 파일 다운로드
		@GetMapping("/noticeFileDownload")
		public ResponseEntity<?> downloadFile(
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("noticeId") long noticeId,
				@RequestParam("noticeFileId") long noticeFileId) {

			try {
				// 다운로드에 필요한 파일 DB 정보 확보
				NoticeFileDto noticeFile = noticeService.findWorkspaceFile(
						workspace.getWorkspaceId(), noticeId, noticeFileId);

				return storageService.downloadFile(noticeFile.getFilePath(), noticeFile.getStoredName(), noticeFile.getOriginalName());
			} catch (StorageFileNotFoundException e) {
				log.error("downloadFile - StorageFileNotFoundException", e);
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.header("Content-Type", "text/plain;charset=UTF-8")
						.body("첨부파일을 찾을 수 없습니다.");
				
			// StorageException : 파일처리 실패 예외
			} catch (StorageException e) {
				log.error("downloadFile - StorageException", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.header("Content-Type", "text/plain;charset=UTF-8")
						.body("첨부파일을 처리할 수 없습니다.");
				
			} catch (DataAccessException e) {
				log.error("downloadFile - DataAccessException", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.header("Content-Type", "text/plain;charset=UTF-8")
						.body("첨부파일 정보를 조회할 수 없습니다.");
				
			} catch (Exception e) {
				log.error("공지 첨부파일 다운로드 중 오류가 발생했습니다.", e);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.header("Content-Type", "text/plain;charset=UTF-8")
						.body("첨부파일을 다운로드할 수 없습니다.");
			}
		}
		
		// 공지사항 수정
		@GetMapping("/noticeEdit")
		public String noticeEdit(
				@RequestAttribute("workspace") WorkspaceDto workspace,
				@RequestParam("noticeId") long noticeId, Model model,
				@ModelAttribute(value = "manageWorkspaceRole", binding = false) int workspaceRole,
				HttpServletResponse response) {
			
			// 공지 수정은 DT만 허용
			if (workspaceRole != 50) {
				response.setStatus(HttpStatus.FORBIDDEN.value());
				model.addAttribute("title", "접근 권한이 없습니다.");
				model.addAttribute("message", "공지 수정은 DT만 가능합니다.");
				return "error/error2";
			}

			try {
				NoticeDto notice = noticeService.findWorkspaceDetailNotice(
						workspace.getWorkspaceId(), noticeId);

				if (notice == null) {
					response.setStatus(HttpStatus.NOT_FOUND.value());
					model.addAttribute("title", "자료를 찾을 수 없습니다.");
					model.addAttribute("message", "공지를 찾을 수 없습니다.");
					return "error/error2";
				}
				
				// 작성자 본인만 수정 화면에 접근 가능
				if (!Long.valueOf(workspace.getMemberId()).equals(notice.getMemberId())) {
					response.setStatus(HttpStatus.FORBIDDEN.value());
					model.addAttribute("title", "접근 권한이 없습니다.");
					model.addAttribute("message", "본인이 작성한 공지만 수정할 수 있습니다.");
					return "error/error2";
				}

				// 파일 목록 조회
				List<NoticeFileDto> noticeFile = noticeService.findWorkspaceFiles(workspace.getWorkspaceId(), noticeId);

				model.addAttribute("noticeFile", noticeFile);
				model.addAttribute("notice", notice);

				return "notice/noticeEdit";
			} catch (DataAccessException e) {
				log.error("noticeEdit - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("공지 수정 화면 조회 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}
		
		// 공지사항 수정 저장
		// RedirectAttributes : 리다이렉트 시 데이터를 숨겨 넘기는 방법을 제공
		@PostMapping("/noticeEdit")
		public String saveNotice(
				@PathVariable("domainKey") String domainKey,
				@RequestAttribute("workspace") WorkspaceDto workspace, 
				@RequestParam("noticeId") long noticeId,
				@RequestParam("title") String title, 
				@RequestParam("content") String content,
				@RequestParam(required = false, name = "files") List<MultipartFile> files,
				@RequestParam(required = false, name = "deleteFileIds") List<Long> deleteFileIds,
				@ModelAttribute(value = "manageWorkspaceRole", binding = false) int workspaceRole,
				RedirectAttributes redirectAttributes,
				Model model,
				HttpServletResponse response) {
			
			// 공지 수정은 DT만 허용
			if (workspaceRole != 50) {
				response.setStatus(HttpStatus.FORBIDDEN.value());
				model.addAttribute("title", "접근 권한이 없습니다.");
				model.addAttribute("message", "공지 수정은 DT만 가능합니다.");
				return "error/error2";
			}

			try {
				
				// 요청으로 받은 작성자 정보 대신 DB에 저장된 작성자 확인
				NoticeDto savedNotice = noticeService.findWorkspaceDetailNotice(
						workspace.getWorkspaceId(), noticeId);

				if (savedNotice == null) {
					response.setStatus(HttpStatus.NOT_FOUND.value());
					model.addAttribute("title", "자료를 찾을 수 없습니다.");
					model.addAttribute("message", "공지를 찾을 수 없습니다.");
					return "error/error2";
				}

				// 작성자 본인만 수정 저장 가능
				if (!Long.valueOf(workspace.getMemberId()).equals(savedNotice.getMemberId())) {
					response.setStatus(HttpStatus.FORBIDDEN.value());
					model.addAttribute("title", "접근 권한이 없습니다.");
					model.addAttribute("message", "본인이 작성한 공지만 수정할 수 있습니다.");
					return "error/error2";
				}
				// 제목 DB에 200자 이내로
				if (title.isBlank() || title.length() > 200 || content.isBlank()) {
					NoticeDto notice = new NoticeDto();

					notice.setNoticeId(noticeId);
					notice.setTitle(title);
					notice.setContent(content);

					List<NoticeFileDto> noticeFile = noticeService.findWorkspaceFiles(workspace.getWorkspaceId(), noticeId);

					model.addAttribute("notice", notice);
					model.addAttribute("noticeFile", noticeFile);
					model.addAttribute("errorMessage", "제목은 200자 이내로 입력하고, 내용은 빈칸없이 입력해 주세요.");
					model.addAttribute("deleteFileIds", deleteFileIds);


					return "notice/noticeEdit";
				}

				// updatedCount 에 수정된 행수를 담는다.
				int updatedCount = noticeService.noticeEdit(
						workspace.getWorkspaceId(), noticeId, title, content, deleteFileIds, files);

				// 수정할 공지가 없을 경우
				if (updatedCount == 0) {
					response.sendError(404, "수정할 공지가 없습니다.");
					return null;
				}

				// 수정 성공 후 이동할 때 공지번호 임시로 전달
				// editedNoticeId : 수정한 공지 번호를 담는다.
				redirectAttributes.addFlashAttribute("editedNoticeId", noticeId);
				
				return "redirect:/" + domainKey + "/home/noticeDetail?noticeId=" + noticeId;
			} catch (IllegalArgumentException e) {
				// 첨부 검증 실패는 입력 내용을 유지한 수정 화면에서 안내
				if (response.isCommitted()) {
					return null;
				}

				NoticeDto notice = new NoticeDto();
				notice.setNoticeId(noticeId);
				notice.setTitle(title);
				notice.setContent(content);

				List<NoticeFileDto> noticeFile = noticeService.findWorkspaceFiles(
						workspace.getWorkspaceId(), noticeId);
				model.addAttribute("notice", notice);
				model.addAttribute("noticeFile", noticeFile);
				model.addAttribute("deleteFileIds", deleteFileIds);
				model.addAttribute("errorMessage", e.getMessage());
				response.setStatus(HttpStatus.BAD_REQUEST.value());
				return "notice/noticeEdit";
			} catch (DataAccessException e) {
				log.error("saveNotice - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (StorageException e) {
				log.error("saveNotice - StorageException", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "파일 처리 오류.");
				model.addAttribute("message", "첨부파일을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("공지 수정 저장 중 오류가 발생했습니다.", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "시스템 오류.");
				model.addAttribute("message", "죄송합니다.<br> <strong>요청을 처리할 수 없습니다.</strong>");
				
				return "error/error2";
			}
		}
		
		// 공지사항 삭제
		@PostMapping("/noticeDelete")
		public String deleteNotice(
				@PathVariable("domainKey") String domainKey,
				@RequestAttribute("workspace") WorkspaceDto workspace, 
				@RequestParam("noticeId") long noticeId,
				@ModelAttribute(value = "manageWorkspaceRole", binding = false) int workspaceRole,
				HttpServletResponse response, Model model,
				RedirectAttributes redirectAttributes) {

			// 공지 삭제는 PM과 DT만 허용
			if (workspaceRole != 70 && workspaceRole != 50) {
				response.setStatus(HttpStatus.FORBIDDEN.value());
				model.addAttribute("title", "접근 권한이 없습니다.");
				model.addAttribute("message", "공지 삭제는 PM과 DT만 가능합니다.");
				return "error/error2";
			}
			
			try {
				// 삭제 대상 공지와 작성자 확인
				NoticeDto savedNotice = noticeService.findWorkspaceDetailNotice(
						workspace.getWorkspaceId(), noticeId);

				if (savedNotice == null) {
					response.setStatus(HttpStatus.NOT_FOUND.value());
					model.addAttribute("title", "자료를 찾을 수 없습니다.");
					model.addAttribute("message", "공지를 찾을 수 없습니다.");
					return "error/error2";
				}

				// DT는 본인 글만 삭제 가능하며, PM은 모든 공지 삭제 가능
				if (workspaceRole == 50
						&& !Long.valueOf(workspace.getMemberId()).equals(savedNotice.getMemberId())) {
					response.setStatus(HttpStatus.FORBIDDEN.value());
					model.addAttribute("title", "접근 권한이 없습니다.");
					model.addAttribute("message", "DT는 본인이 작성한 공지만 삭제할 수 있습니다.");
					return "error/error2";
				}
				
				NoticeDeleteResultDto deleteResult = noticeService.deleteNotice(workspace.getWorkspaceId(), noticeId);
				int deletedCount = deleteResult.getDeletedCount();

				// 삭제할 공지가 없을 경우
				if (deletedCount == 0) {
					response.sendError(404, "삭제할 공지가 없습니다.");
					return null;
				}

				// 공지 삭제 성공 후 실제 회수량이 있을 때만 토스트 메시지 전달
				if (deletedCount == 1 && deleteResult.getRevokedExp() > 0) {
					redirectAttributes.addFlashAttribute(
							"growthExpMessage",
							"공지 삭제로 " + deleteResult.getRevokedExp() + " EXP가 회수되었습니다.");
				}

				// 삭제한 공지는 수정 화면에서 다시 조회할 수 없기 때문에 수정과 리다이렉트 주소가 다름
				return "redirect:/" + domainKey + "/home/notice";
			} catch (DataAccessException e) {
				log.error("deleteNotice - DataAccessException", e);
				if (response.isCommitted()) {
					return null;
				}
				
				response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				model.addAttribute("title", "데이터 처리 오류.");
				model.addAttribute("message", "데이터를 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");
				
				return "error/error2";
				
			} catch (Exception e) {
				log.error("공지 삭제 중 오류가 발생했습니다.", e);
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
