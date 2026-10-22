package com.tk.app.service;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    @Value("${file.upload-root}/notice")
    private String uploadPath;

    // 현재 워크스페이스 역할 조회
    @Override
    public int selectWorkspaceRole(Map<String, Object> map) throws SQLException {
        int result = 0;
        try {
            Integer role = mapper.selectWorkspaceRole(map);
            result = role == null ? 0 : role;
        } catch (Exception e) {
            log.info("selectWorkspaceRole : ", e);
            throw e;
        }
        return result;
    }

    // 프로젝트 선택 목록
    @Override
    public List<ManageNoticeDto> selectProjectOptions(Map<String, Object> map) throws SQLException {
        List<ManageNoticeDto> result = new ArrayList<>();
        try {
            result = mapper.selectProjectOptions(map);
        } catch (Exception e) {
            log.info("selectProjectOptions : ", e);
            throw e;
        }
        return result;
    }

    // 업무 선택 목록
    @Override
    public List<ManageNoticeDto> selectTaskOptions(Map<String, Object> map) throws SQLException {
        List<ManageNoticeDto> result = new ArrayList<>();
        try {
            result = mapper.selectTaskOptions(map);
        } catch (Exception e) {
            log.info("selectTaskOptions : ", e);
            throw e;
        }
        return result;
    }

    // 공지사항 목록 조회
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

    // 공지사항 전체 개수
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

    // 공지사항 상세 조회
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

    // 조회수 증가
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

    // 공지사항 등록
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

    // 공지사항 수정
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

    // 공지사항 논리 삭제
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

    // 첨부파일 목록
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

    // 첨부파일 등록
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

    // 첨부파일 단건 삭제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteNoticeFile(Map<String, Object> map) throws SQLException {
        int result = 0;
        try {
            ManageNoticeDto file = mapper.selectNoticeFile(map);
            if (file != null) {
                result = mapper.deleteNoticeFile(map);
                if (result > 0) {
                    deleteAfterCommit(List.of(file));
                }
            }
        } catch (Exception e) {
            log.info("deleteNoticeFile : ", e);
            throw e;
        }
        return result;
    }

    // 공지사항 및 첨부파일 일괄 등록
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createNotice(ManageNoticeDto dto, List<MultipartFile> files) throws Exception {
        int result = 0;
        List<String> uploaded = new ArrayList<>();
        try {
            validateFiles(files, 0);
            cleanupAfterRollback(uploaded);

            result = mapper.insertNotice(dto);
            if (result != 1) {
                throw new IllegalStateException("공지 등록 권한이 없거나 대상이 잘못되었습니다.");
            }

            saveFiles(dto, files, uploaded);
        } catch (Exception e) {
            log.info("createNotice : ", e);
            throw e;
        }
        return result;
    }

    // 공지사항 수정 및 첨부파일 변경
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int editNotice(ManageNoticeDto dto, List<Long> deleteFileIds,
            List<MultipartFile> files) throws Exception {
        int result = 0;
        List<String> uploaded = new ArrayList<>();
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", dto.getDomainKey());
            map.put("memberId", dto.getRequester_member_id());
            map.put("requester_member_id", dto.getRequester_member_id());
            map.put("notice_id", dto.getNotice_id());

            List<ManageNoticeDto> existingFiles = mapper.selectNoticeFiles(map);
            List<ManageNoticeDto> removedFiles = new ArrayList<>();

            if (deleteFileIds != null) {
                for (Long fileId : deleteFileIds) {
                    boolean found = false;
                    for (ManageNoticeDto file : existingFiles) {
                        if (file.getNotice_file_id().equals(fileId) && !removedFiles.contains(file)) {
                            removedFiles.add(file);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new IllegalArgumentException("삭제할 첨부파일이 올바르지 않습니다.");
                    }
                }
            }

            validateFiles(files, existingFiles.size() - removedFiles.size());
            cleanupAfterRollback(uploaded);

            result = mapper.updateNotice(dto);
            if (result != 1) {
                throw new IllegalStateException("공지 수정 권한이 없거나 대상이 없습니다.");
            }

            saveFiles(dto, files, uploaded);

            for (ManageNoticeDto file : removedFiles) {
                map.put("notice_file_id", file.getNotice_file_id());
                if (mapper.deleteNoticeFile(map) != 1) {
                    throw new IllegalStateException("첨부파일 삭제에 실패했습니다.");
                }
            }

            deleteAfterCommit(removedFiles);
        } catch (Exception e) {
            log.info("editNotice : ", e);
            throw e;
        }
        return result;
    }

    // 첨부파일 유효성 검사
    private void validateFiles(List<MultipartFile> files, int existingCount) {
        Set<String> extensions = Set.of(
                "gif", "png", "txt", "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"
        );
        int count = existingCount;

        if (files == null) {
            return;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            count++;
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank()
                    || name.getBytes(StandardCharsets.UTF_8).length > 255) {
                throw new IllegalArgumentException("첨부파일 이름이 올바르지 않습니다.");
            }

            int dot = name.lastIndexOf('.');
            String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (!extensions.contains(ext)) {
                throw new IllegalArgumentException("허용되지 않은 파일 형식입니다.");
            }
        }

        if (count > 5) {
            throw new IllegalArgumentException("첨부파일은 최대 5개입니다.");
        }
    }

    // 첨부파일 저장
    private void saveFiles(ManageNoticeDto dto, List<MultipartFile> files,
            List<String> uploaded) throws Exception {
        if (files == null) {
            return;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            String storedName = storageService.uploadFileToServer(file, uploadPath);
            if (storedName == null || storedName.isBlank()) {
                throw new IllegalStateException("파일 업로드에 실패했습니다.");
            }
            uploaded.add(storedName);

            ManageNoticeDto fileDto = new ManageNoticeDto();
            fileDto.setNotice_id(dto.getNotice_id());
            fileDto.setDomainKey(dto.getDomainKey());
            fileDto.setRequester_member_id(dto.getRequester_member_id());
            fileDto.setOriginal_name(file.getOriginalFilename());
            fileDto.setStored_name(storedName);
            fileDto.setFile_path(uploadPath);
            fileDto.setFile_size(file.getSize());

            if (mapper.insertNoticeFile(fileDto) != 1) {
                throw new IllegalStateException("파일 정보 저장에 실패했습니다.");
            }
        }
    }

    // 트랜잭션 실패 시 새 파일 제거
    private void cleanupAfterRollback(List<String> uploaded) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    for (String filename : uploaded) {
                        storageService.deleteFile(uploadPath, filename);
                    }
                }
            }
        });
    }

    // 트랜잭션 성공 후 삭제한 첨부파일 제거
    private void deleteAfterCommit(List<ManageNoticeDto> files) {
        if (files.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (ManageNoticeDto file : files) {
                    try {
                        storageService.deleteFile(file.getFile_path(), file.getStored_name());
                    } catch (Exception e) {
                        log.info("deleteAfterCommit : ", e);
                    }
                }
            }
        });
    }
}
