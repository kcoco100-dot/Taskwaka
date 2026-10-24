package com.tk.app.common;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import com.tk.app.domain.dto.ManageFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ManageFileStorage {
    private final StorageService storageService;
    // 첨부파일 저장
    public List<ManageFileDto> save(List<MultipartFile> files, String directory) throws Exception {
        List<ManageFileDto> result = new ArrayList<>();
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                // 실패한 저장 파일 정리
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        remove(result);
                    }
                }
            });
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    String name = file.getOriginalFilename();
                    if (name == null) {
                        throw new IllegalArgumentException("첨부파일 이름이 없습니다.");
                    }
                    name = name.replace('\\', '/');
                    name = name.substring(name.lastIndexOf('/') + 1);
                    if (name.isBlank() || name.getBytes(StandardCharsets.UTF_8).length > 255) {
                        throw new IllegalArgumentException("첨부파일 이름은 255바이트 이내로 입력하세요.");
                    }
                    
                    String storageName = name.contains(".") ? name : name + ".bin";
                    ManageFileDto dto = new ManageFileDto();
                    MultipartFile upload = new MultipartFile() {
                        @Override public String getName() { return file.getName(); }
                        @Override public String getOriginalFilename() { return storageName; }
                        @Override public String getContentType() { return file.getContentType(); }
                        @Override public boolean isEmpty() { return file.isEmpty(); }
                        @Override public long getSize() { return file.getSize(); }
                        @Override public byte[] getBytes() throws java.io.IOException { return file.getBytes(); }
                        @Override public java.io.InputStream getInputStream() throws java.io.IOException { return file.getInputStream(); }
                        @Override public void transferTo(java.io.File destination) throws java.io.IOException { file.transferTo(destination); }
                    };
                    dto.setStoredName(storageService.uploadFileToServer(upload, directory));
                    if (dto.getStoredName() == null || dto.getStoredName().isBlank()) {
                        throw new IllegalStateException("첨부파일 저장에 실패했습니다.");
                    }
                    dto.setOriginalName(name);
                    dto.setPath(directory);
                    dto.setFileSize(file.getSize());
                    result.add(dto);
                }
            }
        } catch (Exception e) {
            log.info("save : ", e);
            throw e;
        }
        return result;
    }
    // 반영 후 첨부파일 삭제 예약
    public void deleteAfterCommit(List<ManageFileDto> files) {
        if (files.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // 반영된 삭제 파일 정리
            @Override
            public void afterCommit() { remove(files); }
        });
    }
    // 첨부파일 삭제
    private void remove(List<ManageFileDto> files) {
        for (ManageFileDto file : files) {
            try {
                if (!storageService.deleteFile(file.getPath(), file.getStoredName())) {
                    log.info("첨부파일 정리 확인 필요: {}", file.getStoredName());
                }
            } catch (Exception e) {
                log.info("remove : ", e);
            }
        }
    }
}
