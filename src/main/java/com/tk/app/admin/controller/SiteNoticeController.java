package com.tk.app.admin.controller;

import java.nio.file.Paths;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.admin.domain.dto.SiteNoticeDto;
import com.tk.app.admin.domain.dto.SiteNoticeFileDto;
import com.tk.app.admin.service.SiteNoticeService;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
public class SiteNoticeController {
	private final SiteNoticeService siteNoticeService;
	private final PaginateUtil paginateUtil;
	private final StorageService storageService;

	@GetMapping("/notice")
	public String noticeList(Model model,
			@RequestParam (defaultValue = "") String keyword,
			@RequestParam(name = "page", defaultValue = "1") int current_page) {
		try {
			int size = 10;
			
			int dataCount = siteNoticeService.dataCountNotice(keyword);
			int totalPage = paginateUtil.pageCount(dataCount, size);
			int offset = (current_page - 1) * size;
			
			List<SiteNoticeDto> noticeList = siteNoticeService.selectNoticePage(keyword, offset, size);
			
			String listUrl = "/admin/notice?keyword=" + keyword; 
			
			String paging = paginateUtil.paging(current_page, totalPage, listUrl);

			model.addAttribute("paging", paging);
;			model.addAttribute("keyword", keyword);
			model.addAttribute("noticeList", noticeList);
			model.addAttribute("activeSection", "notice");
	        model.addAttribute("adminName", "관리자 (ADMIN)");
	        
		} catch (Exception e) {
			log.error("notice 오류 : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/notice")
	public String insertNotice(SiteNoticeDto dto,
			@RequestParam (name = "files", required = false) MultipartFile[] files) {
		try {
			SessionInfo sessionInfo = LoginMemberUtil.getSessionInfo();
			
			if(sessionInfo == null) {
				return "redirect:/member/login";
			}
			
			dto.setMember_id(sessionInfo.getMember_id());
			
			siteNoticeService.insertNotice(dto);
			
			long notice_id = dto.getNotice_id();
			
			if(files != null) {
				for(MultipartFile file : files) {
					
					if(file.isEmpty()) {
						continue;
					}
					// 보관함 위치 찾기
					String directoryPath =
					        Paths.get(System.getProperty("user.home"),
					                "taskwaka", "uploads", "admin", "notice")
					             .toString();
					// 보관함 위치 저장하고, 새 파일 이름 받아오기
					String storedName = storageService.uploadFileToServer(file, directoryPath);
					
					if (storedName == null) {
					    throw new IllegalStateException("첨부파일 저장 실패");
					}
					
					SiteNoticeFileDto fileDto = new SiteNoticeFileDto();
					
					fileDto.setNotice_id(notice_id);
					fileDto.setOriginal_name(file.getOriginalFilename());
					fileDto.setStored_name(storedName);
					fileDto.setFile_path(directoryPath);
					fileDto.setFile_size(file.getSize());
					
					siteNoticeService.insertNoticeFile(fileDto);
				}
			}
			
		} catch (Exception e) {
			log.error("insertNotice 오류 : ", e);
			throw e;
		}
		return "redirect:/admin/notice";
	}
	
	@GetMapping("/notice/{notice_id}")
	public String noticeDetail(@PathVariable long notice_id,
			Model model){
		
		try {
			SiteNoticeDto dto = siteNoticeService.selectNoticeDetail(notice_id);
			List<SiteNoticeFileDto> fileList = siteNoticeService.selectNoticeFiles(notice_id);
			
			model.addAttribute("fileList", fileList);
			model.addAttribute("detail", dto);
			model.addAttribute("activeSection", "noticeDetail");
	        model.addAttribute("adminName", "관리자 (ADMIN)");
			
		} catch (Exception e) {
			log.error("noticeDetail 오류 : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/notice/update")
	public String updateNotice(SiteNoticeDto dto,
			@RequestParam(name = "hideNotice", defaultValue = "false") boolean hideNotice) {
		
		try {
			dto.setStatus(hideNotice ? 0 : 1);
			
			siteNoticeService.updateNotice(dto);
		} catch (Exception e) {
			log.error("updateNotice 오류 : ", e);
			throw e;
		}
		
		return "redirect:/admin/notice";
	}
	
	@PostMapping("/notice/delete")
	public String deleteNotice(@RequestParam long notice_id) {
		try {
			siteNoticeService.deleteNotice(notice_id);
		} catch (Exception e) {
			log.error("deleteNotice 오류 : ", e);
			throw e;
		}
		return "redirect:/admin/notice";
	}
	
	@GetMapping("/notice/write")
	public String noticeWrite(Model model) {
		try {
			model.addAttribute("activeSection", "noticeWrite");
			model.addAttribute("adminName", "관리자 (ADMIN)");
		} catch (Exception e) {
			log.error("noticeWrite 오류 : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/notice/{notice_id}/edit")
	public String noticeEdit(@PathVariable ("notice_id") long notice_id,
			Model model) {
		try {
			SiteNoticeDto detail = siteNoticeService.selectNoticeDetail(notice_id);
			
			model.addAttribute("detail", detail);
			model.addAttribute("activeSection", "noticeEdit");
			model.addAttribute("adminName", "관리자 (ADMIN)");
		} catch (Exception e) {
			log.error("noticeEdit 오류 : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/notice/file/{notice_file_id}/download")
	public ResponseEntity<?> downloadNoticeFile(@PathVariable (name = "notice_file_id") long notice_file_id) {
		try {
			SiteNoticeFileDto fileDto = siteNoticeService.selectNoticeFile(notice_file_id);
			
			if(fileDto == null) {
				return ResponseEntity.notFound().build();
			}
			return storageService.downloadFile(
					fileDto.getFile_path(),
					fileDto.getStored_name(),
					fileDto.getOriginal_name()
			);
		} catch (Exception e) {
			log.error("downloadNoticeFile 오류 : ", e);
			throw e;
		}
	}
}	
