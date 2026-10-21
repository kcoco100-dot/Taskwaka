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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
			log.error("notice : ", e);
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
			log.error("insertNotice : ", e);
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
			log.error("noticeDetail : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/notice/update")
	public String updateNotice(SiteNoticeDto dto,
			@RequestParam(name = "hideNotice", defaultValue = "false") boolean hideNotice,
			@RequestParam(name = "files", required = false) MultipartFile[] files,
			@RequestParam (name = "deleteFileIds", required = false) Long[] deleteFileIds) {
		
		try {
			dto.setStatus(hideNotice ? 0 : 1);
			
			siteNoticeService.updateNotice(dto);
			
			if(deleteFileIds != null) {
				for(Long deleteFileId : deleteFileIds) {
					SiteNoticeFileDto fileDto = siteNoticeService.selectNoticeFile(deleteFileId);
					
					if(fileDto != null && fileDto.getNotice_id() == dto.getNotice_id()) {
						boolean deleted = storageService.deleteFile(fileDto.getFile_path(), fileDto.getStored_name());
					
						if(deleted) {
							siteNoticeService.deleteNoticeFile(fileDto.getNotice_file_id());
							
						}
						
					}	
				}
			}
			
			if(files != null) {
				String directoryPath = Paths.get(
				        System.getProperty("user.home"),
				        "taskwaka", "uploads", "admin", "notice").toString();
				for(MultipartFile file : files) {
					if(file.isEmpty()) {
						continue;
					}
					String storName = storageService.uploadFileToServer(file, directoryPath);
					
					SiteNoticeFileDto fileDto = new SiteNoticeFileDto();
					
					fileDto.setNotice_id(dto.getNotice_id());
					fileDto.setOriginal_name(file.getOriginalFilename());
					fileDto.setStored_name(storName);
					fileDto.setFile_path(directoryPath);
					fileDto.setFile_size(file.getSize());
					
					siteNoticeService.insertNoticeFile(fileDto);
				}
			}
		} catch (Exception e) {
			log.error("updateNotice : ", e);
			throw e;
		}
		
		return "redirect:/admin/notice";
	}
	
	@PostMapping("/notice/delete")
	public String deleteNotice(@RequestParam long notice_id) {
		try {
			List<SiteNoticeFileDto> fileList = siteNoticeService.selectNoticeFiles(notice_id);
			
			for(SiteNoticeFileDto fileDto : fileList) {
				if(fileDto != null) {
					boolean deleted = storageService.deleteFile(fileDto.getFile_path(),fileDto.getStored_name());
					
					if(deleted) {
						siteNoticeService.deleteNoticeFile(fileDto.getNotice_file_id());
					} else {
						throw new IllegalStateException("첨부파일 삭제 실패");
					}
				}
			}
			siteNoticeService.deleteNotice(notice_id);
		} catch (Exception e) {
			log.error("deleteNotice : ", e);
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
			log.error("noticeWrite : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/notice/{notice_id}/edit")
	public String noticeEdit(@PathVariable ("notice_id") long notice_id,
			Model model) {
		try {
			SiteNoticeDto detail = siteNoticeService.selectNoticeDetail(notice_id);
			List<SiteNoticeFileDto> fileList = siteNoticeService.selectNoticeFiles(notice_id);
			
			model.addAttribute("fileList", fileList);
			model.addAttribute("detail", detail);
			model.addAttribute("activeSection", "noticeEdit");
			model.addAttribute("adminName", "관리자 (ADMIN)");
		} catch (Exception e) {
			log.error("noticeEdit : ", e);
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
			log.error("downloadNoticeFile : ", e);
			throw e;
		}
	}
	
	@GetMapping("/notice/{notice_id}/downloadAll")
	public ResponseEntity<StreamingResponseBody> downloadNoticeFiles(@PathVariable (name = "notice_id") long notice_id) {
		try {
			List<SiteNoticeFileDto> fileList = siteNoticeService.selectNoticeFiles(notice_id);
			
			if(fileList == null || fileList.isEmpty()) {
				return ResponseEntity.notFound().build();
			}
			String[] sources = new String[fileList.size()];
			String[] originals = new String[fileList.size()];
			
			for(int i = 0; i < fileList.size(); i++) {
				SiteNoticeFileDto fileDto = fileList.get(i);
				
				sources[i] = Paths.get(fileDto.getFile_path(), fileDto.getStored_name()).toString();
				originals[i] = fileDto.getOriginal_name();
			}
			return storageService.downloadZipFile(sources, originals, "notice_" + notice_id + ".zip");
		} catch (Exception e) {
			log.error("downloadNoticeFiles: ", e);
			throw e;
		}
	}
	
	@PostMapping("/notice/deleteFile")
	public ResponseEntity<?> deleteNoticeFile(@RequestParam long notice_file_id) {
		try {
			SiteNoticeFileDto fileDto = siteNoticeService.selectNoticeFile(notice_file_id);
			
			if(fileDto == null) {
				return ResponseEntity.notFound().build();
			}
			
			boolean deleted  = storageService.deleteFile(fileDto.getFile_path(), fileDto.getStored_name());
			
			if (deleted) {
				siteNoticeService.deleteNoticeFile(notice_file_id);
			    return ResponseEntity.ok().build();
			} else {
			    return ResponseEntity.internalServerError().build();
			}
		} catch (Exception e) {
			log.error("deleteNoticeFile : ", e);
			throw e;
		}
	}
}	
