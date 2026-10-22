
package com.tk.app.service;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.ManageNoticeDto;
import com.tk.app.mapper.ManageNoticeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageNoticeServiceImpl implements ManageNoticeService {

    private final ManageNoticeMapper mapper;
    private final StorageService storageService;

    @Value("${file.upload-root}")
    private String uploadPath;

    // 공지 목록 조회
    @Override
    public List<ManageNoticeDto> selectNoticeList(Map<String, Object> map) throws SQLException {
        List<ManageNoticeDto> result = new ArrayList<>();
        try {
            result = mapper.selectNoticeList(map);
        } catch (Exception e) {
            log.info("selectNoticeList : ", e);
            throw e;
        }
        return result;
    }

    // 공지 개수 조회
    @Override
    public int dataCount(Map<String, Object> map) throws SQLException {
        int result = 0;
        try {
            result = mapper.dataCount(map);
        } catch (Exception e) {
            log.info("dataCount : ", e);
            throw e;
        }
        return result;
    }

    // 공지 조회수 증가
    @Override
    public int updateViewCount(Map<String, Object> map) throws SQLException {
        int result = 0;
        try {
            result = mapper.updateViewCount(map);
        } catch (Exception e) {
            log.info("updateViewCount : ", e);
            throw e;
        }
        return result;
    }

    // 공지 상세 조회
    @Override
    public ManageNoticeDto selectNoticeDetail(Map<String, Object> map) throws SQLException {
        ManageNoticeDto result = null;
        try {
            result = mapper.selectNoticeDetail(map);
        } catch (Exception e) {
            log.info("selectNoticeDetail : ", e);
            throw e;
        }
        return result;
    }

    // 이전 글 조회
    @Override
    public ManageNoticeDto selectPrevNotice(Map<String, Object> map) throws SQLException {
        ManageNoticeDto result = null;
        try {
            result = mapper.selectPrevNotice(map);
        } catch (Exception e) {
            log.info("selectPrevNotice : ", e);
            throw e;
        }
        return result;
    }

    // 다음 글 조회
    @Override
    public ManageNoticeDto selectNextNotice(Map<String, Object> map) throws SQLException {
        ManageNoticeDto result = null;
        try {
            result = mapper.selectNextNotice(map);
        } catch (Exception e) {
            log.info("selectNextNotice : ", e);
            throw e;
        }
        return result;
    }

    // 첨부파일 단건 조회
    @Override
    public ManageNoticeDto selectNoticeFile(Map<String, Object> map) throws SQLException {
        ManageNoticeDto result = null;
        try {
            result = mapper.selectNoticeFile(map);
        } catch (Exception e) {
            log.info("selectNoticeFile : ", e);
            throw e;
        }
        return result;
    }

    // 첨부파일 목록 조회
    @Override
    public List<ManageNoticeDto> selectNoticeFiles(Map<String, Object> map) throws SQLException {
        List<ManageNoticeDto> result = new ArrayList<>();
        try {
            result = mapper.selectNoticeFiles(map);
        } catch (Exception e) {
            log.info("selectNoticeFiles : ", e);
            throw e;
        }
        return result;
    }

    // 공지 등록
    @Override
    public int insertNotice(ManageNoticeDto dto) throws SQLException {
        int result = 0;
        try {
            result = mapper.insertNotice(dto);
        } catch (Exception e) {
            log.info("insertNotice : ", e);
            throw e;
        }
        return result;
    }

    // 공지 수정
    @Override
    public int updateNotice(ManageNoticeDto dto) throws SQLException {
        int result = 0;
        try {
            result = mapper.updateNotice(dto);
        } catch (Exception e) {
            log.info("updateNotice : ", e);
            throw e;
        }
        return result;
    }

    // 공지 삭제 (실제 행을 삭제하지 않고 status = 0으로 변경)
    @Override
    public int deleteNotice(Map<String, Object> map) throws SQLException {
        int result = 0;
        try {
            result = mapper.deleteNotice(map);
        } catch (Exception e) {
            log.info("deleteNotice : ", e);
            throw e;
        }
        return result;
    }

    // 첨부파일 정보 등록
    @Override
    public int insertNoticeFile(ManageNoticeDto dto) throws SQLException {
        int result = 0;
        try {
            result = mapper.insertNoticeFile(dto);
        } catch (Exception e) {
            log.info("insertNoticeFile : ", e);
            throw e;
        }
        return result;
    }

    // 공지 등록 및 첨부파일 업로드
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createNotice(ManageNoticeDto dto, List<MultipartFile> files) throws Exception {
        int result = 0;
        List<String> storedNames = new ArrayList<>();

        try {
            // 첨부파일 검증
            String errorMessage = validateManageNoticeFiles(files);
            if (errorMessage != null) {
                throw new Exception(errorMessage);
            }

            // 공지 등록
            result = mapper.insertNotice(dto);
            if (result != 1 || dto.getNotice_id() <= 0) {
                throw new Exception("공지를 등록할 수 없습니다.");
            }

            // 첨부파일 업로드 및 DB 저장
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }

                    String storedName = storageService.uploadFileToServer(file, uploadPath);
                    if (storedName == null || storedName.isBlank()) {
                        throw new Exception("첨부파일을 저장할 수 없습니다.");
                    }
                    storedNames.add(storedName);

                    ManageNoticeDto fileDto = new ManageNoticeDto();
                    fileDto.setNotice_id(dto.getNotice_id());
                    fileDto.setDomainKey(dto.getDomainKey());
                    fileDto.setRequester_member_id(dto.getRequester_member_id());
                    fileDto.setOriginal_name(file.getOriginalFilename());
                    fileDto.setStored_name(storedName);
                    fileDto.setFile_path(uploadPath);
                    fileDto.setFile_size(file.getSize());

                    int fileResult = mapper.insertNoticeFile(fileDto);
                    if (fileResult != 1) {
                        throw new Exception("첨부파일 정보를 등록할 수 없습니다.");
                    }
                }
            }
        } catch (Exception e) {
            cleanupUploadedFiles(storedNames);
            log.info("createNotice : ", e);
            throw e;
        }
        return result;
    }

    // 공지 수정 및 첨부파일 변경
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int editNotice(ManageNoticeDto dto, List<Long> deleteFileIds,
                          List<MultipartFile> files) throws Exception {
        int result = 0;
        List<String> storedNames = new ArrayList<>();
        List<ManageNoticeDto> deleteFiles = new ArrayList<>();

        try {
            // 현재 공지의 첨부파일 조회
            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("notice_id", dto.getNotice_id());
            fileMap.put("domainKey", dto.getDomainKey());
            fileMap.put("memberId", dto.getRequester_member_id());

            List<ManageNoticeDto> currentFiles = mapper.selectNoticeFiles(fileMap);
            if (currentFiles == null) {
                currentFiles = new ArrayList<>();
            }

            // 삭제할 첨부파일 번호 검증
            Set<Long> deleteIds = new HashSet<>();
            if (deleteFileIds != null) {
                for (Long fileId : deleteFileIds) {
                    if (fileId == null || fileId <= 0) {
                        throw new Exception("삭제할 첨부파일 번호가 올바르지 않습니다.");
                    }
                    if (!deleteIds.add(fileId)) {
                        throw new Exception("삭제할 첨부파일 번호가 중복되었습니다.");
                    }

                    ManageNoticeDto target = null;
                    for (ManageNoticeDto currentFile : currentFiles) {
                        if (fileId.longValue() == currentFile.getNotice_file_id()) {
                            target = currentFile;
                            break;
                        }
                    }

                    if (target == null) {
                        throw new Exception("현재 공지에 속하지 않는 첨부파일입니다.");
                    }
                    deleteFiles.add(target);
                }
            }

            // 새 첨부파일 검증
            String errorMessage = validateManageNoticeFiles(files);
            if (errorMessage != null) {
                throw new Exception(errorMessage);
            }

            // 수정 후 전체 첨부파일 개수 확인
            int newFileCount = 0;
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        newFileCount++;
                    }
                }
            }

            int totalFileCount = currentFiles.size() - deleteFiles.size() + newFileCount;
            if (totalFileCount > 5) {
                throw new Exception("첨부파일은 5개까지 등록할 수 있습니다.");
            }

            // 새 첨부파일 업로드
            List<ManageNoticeDto> newFiles = new ArrayList<>();
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) {
                        continue;
                    }

                    String storedName = storageService.uploadFileToServer(file, uploadPath);
                    if (storedName == null || storedName.isBlank()) {
                        throw new Exception("첨부파일을 저장할 수 없습니다.");
                    }
                    storedNames.add(storedName);

                    ManageNoticeDto fileDto = new ManageNoticeDto();
                    fileDto.setNotice_id(dto.getNotice_id());
                    fileDto.setDomainKey(dto.getDomainKey());
                    fileDto.setRequester_member_id(dto.getRequester_member_id());
                    fileDto.setOriginal_name(file.getOriginalFilename());
                    fileDto.setStored_name(storedName);
                    fileDto.setFile_path(uploadPath);
                    fileDto.setFile_size(file.getSize());
                    newFiles.add(fileDto);
                }
            }

            // 공지 본문 수정
            result = mapper.updateNotice(dto);
            if (result != 1) {
                throw new Exception("공지를 수정할 수 없습니다.");
            }

            // 새 첨부파일 DB 등록
            for (ManageNoticeDto fileDto : newFiles) {
                int fileResult = mapper.insertNoticeFile(fileDto);
                if (fileResult != 1) {
                    throw new Exception("첨부파일 정보를 등록할 수 없습니다.");
                }
            }

            // 기존 첨부파일 DB 삭제
            for (ManageNoticeDto deleteFile : deleteFiles) {
                Map<String, Object> deleteMap = new HashMap<>();
                deleteMap.put("notice_id", dto.getNotice_id());
                deleteMap.put("notice_file_id", deleteFile.getNotice_file_id());
                deleteMap.put("domainKey", dto.getDomainKey());
                deleteMap.put("requester_member_id", dto.getRequester_member_id());

                int deleteResult = mapper.deleteNoticeFile(deleteMap);
                if (deleteResult != 1) {
                    throw new Exception("첨부파일 정보를 삭제할 수 없습니다.");
                }
            }

            // DB 트랜잭션이 정상적으로 커밋된 뒤 실제 파일 삭제
            deleteFilesAfterCommit(deleteFiles, "editNotice");
        } catch (Exception e) {
            cleanupUploadedFiles(storedNames);
            log.info("editNotice : ", e);
            throw e;
        }
        return result;
    }

    // 첨부파일 정보 및 실제 파일 삭제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNoticeFile(Map<String, Object> map) throws SQLException {
        int result = 0;
        try {
            // 첨부파일 조회에 필요한 회원 키 설정
            Map<String, Object> readMap = new HashMap<>(map);
            if (readMap.get("memberId") == null) {
                readMap.put("memberId", readMap.get("requester_member_id"));
            }

            // 삭제할 첨부파일 조회
            ManageNoticeDto file = mapper.selectNoticeFile(readMap);
            if (file == null) {
                return result;
            }

            // DB 메타데이터 삭제
            result = mapper.deleteNoticeFile(map);

            // DB 삭제가 성공한 경우 커밋 후 실제 파일 삭제
            if (result == 1) {
                List<ManageNoticeDto> deleteFiles = new ArrayList<>();
                deleteFiles.add(file);
                deleteFilesAfterCommit(deleteFiles, "deleteNoticeFile");
            }
        } catch (Exception e) {
            log.info("deleteNoticeFile : ", e);
            throw e;
        }
        return result;
    }

    // 첨부파일 검증: 파일명, 확장자, 개수 확인
    private String validateManageNoticeFiles(List<MultipartFile> files) {
        if (files == null) {
            return null;
        }

        int fileCount = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            fileCount++;

            String originalName = file.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) {
                return "첨부파일 이름을 확인해 주세요.";
            }

            if (originalName.getBytes(StandardCharsets.UTF_8).length > 255) {
                return "첨부 파일명이 너무 깁니다. 파일명을 줄여 주세요.";
            }

            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex <= 0 || dotIndex == originalName.length() - 1) {
                return "확장자가 있는 파일만 첨부 가능합니다.";
            }

            String extension = originalName.substring(dotIndex);
            if (!extension.equalsIgnoreCase(".gif")
                    && !extension.equalsIgnoreCase(".png")
                    && !extension.equalsIgnoreCase(".txt")
                    && !extension.equalsIgnoreCase(".pdf")
                    && !extension.equalsIgnoreCase(".doc")
                    && !extension.equalsIgnoreCase(".docx")
                    && !extension.equalsIgnoreCase(".ppt")
                    && !extension.equalsIgnoreCase(".pptx")
                    && !extension.equalsIgnoreCase(".xls")
                    && !extension.equalsIgnoreCase(".xlsx")) {
                return "허용되지 않은 첨부파일 형식입니다.";
            }
        }

        if (fileCount > 5) {
            return "첨부파일은 5개까지 등록할 수 있습니다.";
        }

        return null;
    }

    // DB 커밋 이후 기존 첨부파일의 실제 파일 삭제
    private void deleteFilesAfterCommit(List<ManageNoticeDto> files, String methodName) {
        Runnable deleteAction = () -> {
            for (ManageNoticeDto file : files) {
                deletePhysicalFile(file, methodName);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteAction.run();
                }
            });
        } else {
            deleteAction.run();
        }
    }

    // 업로드 실패 시 새로 업로드한 파일 정리
    private void cleanupUploadedFiles(List<String> storedNames) {
        for (String storedName : storedNames) {
            try {
                boolean deleted = storageService.deleteFile(uploadPath, storedName);
                if (!deleted) {
                    log.info("cleanupUploadedFiles : 첨부파일 정리 실패 - {}", storedName);
                }
            } catch (Exception e) {
                log.info("cleanupUploadedFiles : ", e);
            }
        }
    }

    // 실제 첨부파일 삭제
    private void deletePhysicalFile(ManageNoticeDto file, String methodName) {
        if (file == null || file.getStored_name() == null || file.getStored_name().isBlank()) {
            log.info("{} : 저장 파일명이 없어 실제 파일 삭제를 건너뜁니다.", methodName);
            return;
        }

        String filePath = file.getFile_path();
        if (filePath == null || filePath.isBlank()) {
            filePath = uploadPath;
        }

        try {
            boolean deleted = storageService.deleteFile(filePath, file.getStored_name());
            if (!deleted) {
                log.info("{} : 실제 파일 삭제 실패 - {}", methodName, file.getStored_name());
            }
        } catch (Exception e) {
            log.info(methodName + " : ", e);
        }
    }
}
