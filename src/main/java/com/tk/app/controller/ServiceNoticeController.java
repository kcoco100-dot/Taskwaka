package com.tk.app.controller;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tk.app.admin.domain.dto.SiteNoticeDto;
import com.tk.app.admin.domain.dto.SiteNoticeFileDto;
import com.tk.app.admin.service.SiteNoticeService;
import com.tk.app.common.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/service")
public class ServiceNoticeController {
	private final SiteNoticeService siteNoticeService;
	private final StorageService storageService;

	@GetMapping("/notice")
	public String noticeList(Model model) {
		try {

			List<SiteNoticeDto> noticeList = siteNoticeService.selectNoticeList();

			List<SiteNoticeDto> publicList = new ArrayList<>();

			for (SiteNoticeDto dto : noticeList) {
				if (dto.getStatus() == 1) {
					publicList.add(dto);
				}
			}

			model.addAttribute("noticeList", publicList);

			return "service/notice-list";
		} catch (Exception e) {
			log.error("noticeList : ", e);
			throw e;
		}
	}

	@GetMapping("/notice/{notice_id}")
	public String noticeDetail(@PathVariable("notice_id") long notice_id, Model model) {
		try {

			SiteNoticeDto detail = siteNoticeService.selectNoticeDetail(notice_id);

			if (detail == null || detail.getStatus() != 1) {
				model.addAttribute("title", "공지사항을 찾을 수 없습니다.");
				model.addAttribute("message", "삭제되었거나 공개되지 않은 공지사항입니다.");
				return "error/error2";
			}
			model.addAttribute("detail", detail);

			List<SiteNoticeFileDto> fileList = siteNoticeService.selectNoticeFiles(notice_id);

			model.addAttribute("fileList", fileList);

			return "service/notice-detail";
		} catch (Exception e) {
			log.error("noticeDetail : ", e);
			throw e;
		}
	}

	@GetMapping("/notice/file/{notice_file_id}/download")
	public ResponseEntity<?> downloadNoticeFile(@PathVariable("notice_file_id") long notice_file_id) {
		try {
			SiteNoticeFileDto fileDto = siteNoticeService.selectNoticeFile(notice_file_id);

			if (fileDto == null) {
				return ResponseEntity.notFound().build();
			}
			SiteNoticeDto detail = siteNoticeService.selectNoticeDetail(fileDto.getNotice_id());

			if (detail == null || detail.getStatus() != 1) {
				return ResponseEntity.notFound().build();
			}

			return storageService.downloadFile(fileDto.getFile_path(), fileDto.getStored_name(),
					fileDto.getOriginal_name());

		} catch (Exception e) {
			log.error("downloadNoticeFile : ", e);
			throw e;
		}
	}
	
	@GetMapping("/notice/{notice_id}/downloadAll")
	public ResponseEntity<StreamingResponseBody> downloadNoticeFiles(@PathVariable("notice_id") long notice_id) {
	    try {
			SiteNoticeDto detail = siteNoticeService.selectNoticeDetail(notice_id);
			
			if(detail == null || detail.getStatus() != 1) {
				return ResponseEntity.notFound().build();
			}
			
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
	        log.error("downloadNoticeFiles : ", e);
	        throw e;
	    }
	}
}
