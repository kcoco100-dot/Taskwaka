package com.tk.app.editor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

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
			
			if (!isRasterImage(partFile)) {
                return ResponseEntity.badRequest().body(Map.of("message", "PNG, JPEG, GIF, WebP 이미지를 선택하세요."));
            }
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
			return ResponseEntity.internalServerError().build();
		}

	}
    private static boolean isRasterImage(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || file.isEmpty()) return false;
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        try (var input = file.getInputStream()) {
            byte[] head = input.readNBytes(12);
            if (head.length < 12) return false;
            String signature = new String(head, StandardCharsets.ISO_8859_1);
            return switch (ext) {
                case "png" -> Arrays.equals(Arrays.copyOf(head,8),
                        new byte[]{(byte)137,80,78,71,13,10,26,10});
                case "jpg", "jpeg" -> head[0] == (byte)255 && head[1] == (byte)216 && head[2] == (byte)255;
                case "gif" -> signature.startsWith("GIF87a") || signature.startsWith("GIF89a");
                case "webp" -> signature.startsWith("RIFF") && signature.endsWith("WEBP");
                default -> false;
            };
        }
    }
}
