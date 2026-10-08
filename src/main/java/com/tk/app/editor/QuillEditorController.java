package com.tk.app.editor;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.RequestUtils;
import com.tk.app.common.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class QuillEditorController {
	private final StorageService storageService;

	@Value("${file.upload-root}/editor")
	private String uploadPath;
	
	@PostMapping(value = {"/editor/upload", "/api/editor/upload"})
	public ResponseEntity<?> handleImageUpload(@RequestParam(name = "imageFile") MultipartFile partFile) {
		try {
			// Quill(퀼) text editor 이미지 업로드
			
			String saveFilename = Objects.requireNonNull(storageService.uploadFileToServer(partFile, uploadPath));
			
			String cp = RequestUtils.getContextPath();
			String imageUrl = cp + "/uploads/editor/" + saveFilename;

			return ResponseEntity.ok(Map.of(
					"saveFilename", saveFilename, 
					"imageUrl", imageUrl
				));			
		} catch (Exception e) {
			log.info("handleImageUpload : ", e);
			
			// return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
			return ResponseEntity.ok().build();
		}

	}
}
