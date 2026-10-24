package com.tk.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.ManageFileStorage;
import com.tk.app.domain.dto.ManageFileDto;
import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.mapper.ManageReportMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageReportServiceImpl implements ManageReportService {

	private final ManageReportMapper mapper;
	private final GrowthTreeService growthTreeService;
    private final ManageFileStorage fileStorage;
    @Value("${file.upload-root}/reports")
    private String uploadPath;

	// 보고서 목록 조회
	@Override
	public List<ManageReportDto> listReport(Map<String, Object> map) {
		List<ManageReportDto> result = new ArrayList<>();
		try {
			result = mapper.listReport(map);
		} catch (Exception e) {
			log.info("listReport : ", e);
			throw new IllegalStateException("보고서 목록을 조회할 수 없습니다.", e);
		}
		return result;
	}

	// 보고서 건수 조회
	@Override
	public int countReport(Map<String, Object> map) {
		int result = 0;
		try {
			result = mapper.countReport(map);
		} catch (Exception e) {
			log.info("countReport : ", e);
			throw new IllegalStateException("보고서 수를 조회할 수 없습니다.", e);
		}
		return result;
	}

	// 보고서 상세 및 최초 열람 처리
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ManageReportDto openReport(Map<String, Object> map) {
		ManageReportDto result = null;
		try {
			ManageReportDto dto = mapper.reportDetail(map);
			if (dto == null) {
				throw new IllegalArgumentException("보고서가 없거나 조회 권한이 없습니다.");
			}

			map.put("projectId", dto.getProjectId());
			map.put("authorProjectMemberId", dto.getProjectMemberId());
            if (mapper.lockProjectProgress(map) == null) {
                throw new IllegalArgumentException("프로젝트가 없습니다.");
            }
            if (mapper.lockReport(map) == null) {
                throw new IllegalArgumentException("보고서가 없습니다.");
            }
            dto = mapper.reportDetail(map);
            if (dto == null) {
                throw new IllegalArgumentException("보고서를 찾을 수 없습니다.");
            }

			Long reviewerId = mapper.findReviewerProjectMemberId(map);

			if (reviewerId != null && mapper.countReviewButtonVisible(map) > 0 && dto.getReviewStatus() == 1) {
				mapper.lockReport(map);
				dto = mapper.reportDetail(map);

				if (dto == null) {
					throw new IllegalArgumentException("보고서를 찾을 수 없습니다.");
				}

				if (dto.getReviewId() == null) {
					map.put("reviewerProjectMemberId", reviewerId);
					mapper.insertReview(map);
				} else if (dto.getReviewStatus() == 1) {
					map.put("reviewId", dto.getReviewId());
					mapper.updateViewed(map);
				}

				dto = (ManageReportDto) mapper.reportDetail(map);
			}

			if (dto != null) {
				dto.setCanReview(reviewerId != null && mapper.countReviewButtonVisible(map) > 0 && dto.getReviewStatus() == 2 ? 1 : 0);
			}
			result = dto;

		} catch (Exception e) {
			log.info("openReport : ", e);
			throw new IllegalStateException("보고서 상세 조회에 실패했습니다: " + e.getMessage(), e);
		}
		return result;
	}

	// 보고서 승인 및 반려
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateReview(Map<String, Object> map) {
		try {
			Integer status = (Integer) map.get("reviewStatus");
			String rejectReason = (String) map.get("rejectReason");
			String reviewComment = (String) map.get("reviewComment");

			if (status == null || (status != 3 && status != 4)) {
				throw new IllegalArgumentException("승인 또는 반려 상태만 선택할 수 있습니다.");
			}
			if (status == 4 && (rejectReason == null || rejectReason.isBlank())) {
				throw new IllegalArgumentException("반려 사유를 입력하세요.");
			}
			if (rejectReason != null && rejectReason.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1000) {
				throw new IllegalArgumentException("반려 사유는 1000바이트 이내로 입력하세요.");
			}
			if (reviewComment != null && reviewComment.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1000) {
				throw new IllegalArgumentException("검토 의견은 1000바이트 이내로 입력하세요.");
			}

			ManageReportDto dto = mapper.reportDetail(map);
			if (dto == null) {
				throw new IllegalArgumentException("보고서가 없거나 조회 권한이 없습니다.");
			}

			map.put("projectId", dto.getProjectId());
			map.put("authorProjectMemberId", dto.getProjectMemberId());
            if (mapper.lockProjectProgress(map) == null) {
                throw new IllegalArgumentException("프로젝트가 없습니다.");
            }
            if (mapper.lockReport(map) == null) {
                throw new IllegalArgumentException("보고서가 없습니다.");
            }
            dto = mapper.reportDetail(map);
            if (dto == null) {
                throw new IllegalArgumentException("보고서를 찾을 수 없습니다.");
            }
			Long reviewerId = mapper.findReviewerProjectMemberId(map);

			if (reviewerId == null) {
				throw new IllegalArgumentException("보고서 결재 권한이 없습니다.");
			}
			if (dto.getReviewId() == null || dto.getReviewStatus() != 2) {
				throw new IllegalArgumentException("열람 대기 중인 보고서만 처리할 수 있습니다.");
			}

			map.put("reviewId", dto.getReviewId());
            map.put("reviewDbStatus", status - 2);
			map.put("reviewerProjectMemberId", reviewerId);
			if (status == 3) {
				map.put("rejectReason", null);
			}

			int result = mapper.updateReview(map);

			if (result != 1) {
				throw new IllegalStateException(
						"이미 처리되었거나 결재할 수 없는 보고서입니다."
				);
			}
			
			if (dto.getReportCategory() == 2) {
			    if (status == 3 && mapper.countRewardEligibleReviewer(map) == 1) {
			        growthTreeService.grantTeamReportApproveExp(
			                dto.getReportId(),
			                dto.getReviewId(),
			                ((Number) map.get("memberId")).longValue());
			    } else if (status == 4) {
			        growthTreeService.revokeTeamReportApproveExp(
			                ((Number) map.get("workspaceId")).longValue(),
			                dto.getReportId());
			    }
			}

			if (dto.getReportCategory() == 2) {

				map.put("taskId", dto.getTaskId());

				int taskResult = mapper.updateTaskProgress(map);

				if (taskResult != 1) {
					throw new IllegalStateException("업무 진행률 갱신에 실패했습니다.");
				}

				int projectResult = mapper.updateProjectProgress(map);

				if (projectResult != 1) {
					throw new IllegalStateException("프로젝트 진행률 갱신에 실패했습니다.");
				}
			}

		} catch (Exception e) {
			log.info("updateReview : ", e);
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	// 보고서 상태별 건수 조회
	@Override
	public Map<String, Object> countReportStatuses(Map<String, Object> map) {
		Map<String, Object> result = new HashMap<>();
		try {
			result = mapper.countReportStatuses(map);
		} catch (Exception e) {
			log.info("countReportStatuses : ", e);
			throw new IllegalStateException("보고서 결재 상태 조회 실패", e);
		}
		return result;
	}

	// 보고서 등록용 프로젝트 조회
	@Override
	public List<Map<String, Object>> listWritableProjects(Map<String, Object> map) throws Exception {
		List<Map<String, Object>> result = new ArrayList<>();

        try {
            result = mapper.listWritableProjects(map);
        } catch (Exception e) {
            log.info("listWritableProjects : ", e);
            throw e;
        }

        return result;
	}

    // 프로젝트 보고서 조회
    @Override
    public ManageReportDto getReport(Map<String, Object> map) {
        ManageReportDto result = null;
        try {
            result = mapper.reportDetail(map);
            if (result == null) {
                throw new IllegalArgumentException("보고서가 없거나 현재 참여 중인 프로젝트가 아닙니다.");
            }
        } catch (Exception e) {
            log.info("getReport : ", e);
            throw e;
        }
        return result;
    }
    // 보고서 첨부파일 목록 조회
    @Override
    public List<ManageFileDto> listReportFiles(Map<String, Object> map) {
        List<ManageFileDto> result = new ArrayList<>();
        try {
            getReport(map);
            result = mapper.listReportFiles(map);
        } catch (Exception e) {
            log.info("listReportFiles : ", e);
            throw e;
        }
        return result;
    }
    // 이전 보고서 조회
    @Override
    public ManageReportDto selectPrevReport(Map<String, Object> map) {
        ManageReportDto result = null;
        try {
            result = mapper.selectPrevReport(map);
        } catch (Exception e) {
            log.info("selectPrevReport : ", e);
            throw e;
        }
        return result;
    }
    // 다음 보고서 조회
    @Override
    public ManageReportDto selectNextReport(Map<String, Object> map) {
        ManageReportDto result = null;
        try {
            result = mapper.selectNextReport(map);
        } catch (Exception e) {
            log.info("selectNextReport : ", e);
            throw e;
        }
        return result;
    }
    // 보고서 입력값 확인
    private void validateReport(ManageReportDto dto) {
        if (dto.getReportTitle() == null || dto.getReportTitle().isBlank()
                || dto.getReportTitle().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 200) {
            throw new IllegalArgumentException("보고서 제목은 200바이트 이내로 입력하세요.");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new IllegalArgumentException("내용을 입력하세요.");
        }
        Double progress = dto.getProgress();
        if (progress == null || !Double.isFinite(progress) || progress < 0 || progress > 100) {
            throw new IllegalArgumentException("진행률은 0~100 사이로 입력하세요.");
        }
    }
    // 보고서 변경 잠금
    private ManageReportDto lockProjectReport(Map<String, Object> map) {
        map.put("reportCategory", 1);
        ManageReportDto result = getReport(map);
        map.put("projectId", result.getProjectId());
        if (mapper.lockProjectProgress(map) == null || mapper.lockReport(map) == null) {
            throw new IllegalArgumentException("보고서를 찾을 수 없습니다.");
        }
        result = getReport(map);
        return result;
    }
    // 보고서 첨부파일 저장
    private void saveReportFiles(Long reportId, List<MultipartFile> files) throws Exception {
        for (ManageFileDto file : fileStorage.save(files, uploadPath)) {
            file.setReportId(reportId);
            if (mapper.insertReportFile(file) != 1) {
                throw new IllegalStateException("파일 정보 등록에 실패했습니다.");
            }
        }
    }
    // 프로젝트 보고서 등록
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createReport(ManageReportDto dto, Map<String, Object> map, List<MultipartFile> files) throws Exception {
        try {
            validateReport(dto);
            map.put("projectId", dto.getProjectId());
            if (mapper.lockProjectProgress(map) == null) {
                throw new IllegalArgumentException("프로젝트가 없습니다.");
            }
            Long authorId = mapper.findOwnProjectMemberId(map);
            if (authorId == null) {
                throw new IllegalArgumentException("현재 참여 중인 프로젝트만 등록할 수 있습니다.");
            }
            
            dto.setProjectMemberId(authorId);
            if (mapper.insertReport(dto) != 1) {
                throw new IllegalStateException("보고서 등록에 실패했습니다.");
            }
            saveReportFiles(dto.getReportId(), files);
        } catch (Exception e) {
            log.info("createReport : ", e);
            throw e;
        }
    }
    // 프로젝트 보고서 수정
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editReport(ManageReportDto dto, Map<String, Object> map, List<MultipartFile> files, List<Long> deleteFileIds) throws Exception {
        try {
            ManageReportDto existing = lockProjectReport(map);
            validateReport(dto);
            dto.setReportId(existing.getReportId());
            dto.setProjectId(existing.getProjectId());
            List<ManageFileDto> oldFiles = mapper.listReportFiles(map);
            Set<Long> removals = deleteFileIds == null ? Set.of() : new HashSet<>(deleteFileIds);
            List<ManageFileDto> removed = new ArrayList<>();
            for (ManageFileDto file : oldFiles) {
                if (removals.contains(file.getFileId())) {
                    removed.add(file);
                }
            }
            if (removed.size() != removals.size()) {
                throw new IllegalArgumentException("삭제할 첨부파일을 확인하세요.");
            }
            if (mapper.updateReport(dto) != 1) {
                throw new IllegalStateException("보고서 수정에 실패했습니다.");
            }
            mapper.resetReview(map);
            for (ManageFileDto file : removed) {
                map.put("fileId", file.getFileId());
                if (mapper.deleteReportFile(map) != 1) {
                    throw new IllegalStateException("첨부파일 삭제에 실패했습니다.");
                }
            }
            saveReportFiles(existing.getReportId(), files);
            fileStorage.deleteAfterCommit(removed);
        } catch (Exception e) {
            log.info("editReport : ", e);
            throw e;
        }
    }
    // 프로젝트 보고서 삭제
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReport(Map<String, Object> map) throws Exception {
        try {
            lockProjectReport(map);
            List<ManageFileDto> files = mapper.listReportFiles(map);
            for (ManageFileDto file : files) {
                map.put("fileId", file.getFileId());
                if (mapper.deleteReportFile(map) != 1) {
                    throw new IllegalStateException("첨부파일 삭제에 실패했습니다.");
                }
            }
            mapper.deleteReportReviews(map);
            if (mapper.deleteReport(map) != 1) {
                throw new IllegalStateException("보고서 삭제에 실패했습니다.");
            }
            fileStorage.deleteAfterCommit(files);
        } catch (Exception e) {
            log.info("deleteReport : ", e);
            throw e;
        }
    }
}
