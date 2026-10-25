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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.domain.dto.SiteInquiryFileDto;
import com.tk.app.admin.service.SiteInquiryService;
import com.tk.app.common.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin")
public class SiteInquiryController {
	private final SiteInquiryService siteInquiryService;
	private final StorageService storageService;
	
	
	@GetMapping("/inquiry")
	public String selectAllInquiryList(Model model) {
		try {
			List<SiteInquiryDto> inquiryList = siteInquiryService.selectAllInquiryList();
			
			model.addAttribute("inquiryList", inquiryList);
			model.addAttribute("activeSection", "inquiry");
			model.addAttribute("adminName", "관리자 (ADMIN)");
		} catch (Exception e) {
			log.error("selectAllInquiryList : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/inquiry/{inquiry_id}")
	public String selectAdminInquiryDetail(@PathVariable ("inquiry_id") long inquiry_id,
			Model model) {
		try {
			SiteInquiryDto inquiryDetail = siteInquiryService.selectAdminInquiryDetail(inquiry_id);
			
			if(inquiryDetail == null) {
				return "redirect:/admin/inquiry";
			}
			
			List<SiteInquiryFileDto> fileList = siteInquiryService.selectInquiryFileList(inquiry_id);
			
			model.addAttribute("fileList", fileList);
			model.addAttribute("inquiryDetail", inquiryDetail);
			model.addAttribute("activeSection", "inquiryDetail");
			model.addAttribute("adminName", "관리자 (ADMIN)");
			
		} catch (Exception e) {
			log.error("selectAdminInquiryDetail : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/inquiry/answer")
	public String updateInquiryAnswer(SiteInquiryDto dto) {
		try {
			siteInquiryService.updateInquiryAnswer(dto);
		} catch (Exception e) {
			log.error("updateInquiryAnswer : ", e);
			throw e;
		}
		return "redirect:/admin/inquiry";
	}
	
	@PostMapping("/inquiry/{inquiry_id}/delete")
	@ResponseBody
	public String deleteAdminInquiry(@PathVariable ("inquiry_id") long inquiry_id) {
		try {
			SiteInquiryDto dto = new SiteInquiryDto();
			dto.setInquiry_id(inquiry_id);
			
			int result = siteInquiryService.deleteAdminInquiry(dto);
			
			if(result == 0) {
				return "fail";
			}
			
		} catch (Exception e) {
			log.error("deleteAdminInquiry : ", e);
			throw e;
		}
		return "success";
	}
	
	@GetMapping("/inquiry/file/{inquiries_file_id}/download")
	public ResponseEntity<?> downloadInquiryFile(@PathVariable ("inquiries_file_id") long inquiries_file_id) {
		try {
			SiteInquiryFileDto fileDto = siteInquiryService.selectInquiryFile(inquiries_file_id);
			
			if (fileDto == null) {
				return ResponseEntity.notFound().build();
			}
			
			return storageService.downloadFile(fileDto.getFile_path(), fileDto.getStored_name(), fileDto.getOriginal_name());
		} catch (Exception e) {
			log.error("downloadInquiryFile : ", e);
			throw e;
		}
	}
	
	@GetMapping("/inquiry/{inquiry_id}/download-zip")
	public ResponseEntity<StreamingResponseBody> downloadInquiryZip(@PathVariable("inquiry_id") long inquiry_id) {
	    try {
	        SiteInquiryDto inquiry = siteInquiryService.selectAdminInquiryDetail(inquiry_id);
	        if(inquiry == null) {
	            return ResponseEntity.notFound().build();
	        }

	        List<SiteInquiryFileDto> fileList = siteInquiryService.selectInquiryFileList(inquiry_id);

	        if(fileList == null || fileList.isEmpty()) {
	            return ResponseEntity.notFound().build();
	        }

	        String[] sources = new String[fileList.size()];
	        String[] originals = new String[fileList.size()];

	        for(int i = 0; i < fileList.size(); i++) {
	            SiteInquiryFileDto fileDto = fileList.get(i);

	            sources[i] = Paths.get(fileDto.getFile_path(), fileDto.getStored_name()).toString();
	            originals[i] = fileDto.getOriginal_name();
	        }

	        return storageService.downloadZipFile(sources, originals, "inquiry_" + inquiry_id + ".zip");

	    } catch(Exception e) {
	        log.error("downloadInquiryZip : ", e);
	        throw e;
	    }
	}
}
