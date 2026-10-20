package com.tk.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageReportDto;
import com.tk.app.mapper.ManageReportMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageReportServiceImpl implements ManageReportService {

    private final ManageReportMapper mapper;

    // 보고서 목록 조회
    @Override
    public List<ManageReportDto> listReport(Map<String, Object> map) {
        try {
            return mapper.listReport(map);
        } catch (Exception e) {
            log.error("listReport : ", e);
            throw new IllegalStateException("보고서 목록을 조회할 수 없습니다.", e);
        }
    }

    // 보고서 개수 조회
    @Override
    public int countReport(Map<String, Object> map) {
        try {
            return mapper.countReport(map);
        } catch (Exception e) {
            log.error("countReport : ", e);
            throw new IllegalStateException("보고서 수를 조회할 수 없습니다.", e);
        }
    }

    // 보고서 최초 열람 및 결재 가능 여부 조회
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManageReportDto openReport(Map<String, Object> map) {
        try {
            ManageReportDto dto = mapper.reportDetail(map);
            if (dto == null) {
                throw new IllegalArgumentException("보고서가 없거나 조회 권한이 없습니다.");
            }

            map.put("projectId", dto.getProjectId());
            map.put("authorProjectMemberId", dto.getProjectMemberId());

            Long reviewerId = mapper.findReviewerProjectMemberId(map);

            if (reviewerId != null && dto.getReviewStatus() == -1) {
                mapper.lockReport(map);
                dto = mapper.reportDetail(map);

                if (dto == null) {
                    throw new IllegalArgumentException("보고서를 찾을 수 없습니다.");
                }

                if (dto.getReviewId() == null) {
                    map.put("reviewerProjectMemberId", reviewerId);
                    mapper.insertReview(map);
                } else if (dto.getReviewStatus() == -1) {
                    map.put("reviewId", dto.getReviewId());
                    mapper.updateViewed(map);
                }

                dto = (ManageReportDto) mapper.reportDetail(map);
            }

            if (dto != null) {
                dto.setCanReview(reviewerId != null && dto.getReviewStatus() == 0 ? 1 : 0);
            }
            return dto;

        } catch (Exception e) {
            log.error("openReport : ", e);
            throw new IllegalStateException("보고서 상세 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 보고서 승인 및 반려 처리
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReview(Map<String, Object> map) {
        try {
            Integer status = (Integer) map.get("reviewStatus");
            String rejectReason = (String) map.get("rejectReason");
            String reviewComment = (String) map.get("reviewComment");

            if (status == null || (status != 1 && status != 2)) {
                throw new IllegalArgumentException("승인 또는 반려 상태만 선택할 수 있습니다.");
            }
            if (status == 2 && (rejectReason == null || rejectReason.isBlank())) {
                throw new IllegalArgumentException("반려 사유를 입력하세요.");
            }
            if (rejectReason != null && rejectReason.length() > 1000) {
                throw new IllegalArgumentException("반려 사유는 1000자 이내로 입력하세요.");
            }
            if (reviewComment != null && reviewComment.length() > 1000) {
                throw new IllegalArgumentException("검토 의견은 1000자 이내로 입력하세요.");
            }

            ManageReportDto dto = mapper.reportDetail(map);
            if (dto == null) {
                throw new IllegalArgumentException("보고서가 없거나 조회 권한이 없습니다.");
            }

            map.put("projectId", dto.getProjectId());
            map.put("authorProjectMemberId", dto.getProjectMemberId());
            Long reviewerId = mapper.findReviewerProjectMemberId(map);

            if (reviewerId == null) {
                throw new IllegalArgumentException("보고서 결재 권한이 없습니다.");
            }
            if (dto.getReviewId() == null || dto.getReviewStatus() != 0) {
                throw new IllegalArgumentException("열람 대기 중인 보고서만 처리할 수 있습니다.");
            }

            map.put("reviewId", dto.getReviewId());
            map.put("reviewerProjectMemberId", reviewerId);
            if (status == 1) {
                map.put("rejectReason", null);
            }

            if (status == 1 && dto.getReportCategory() == 2) {

                Long projectId = mapper.lockProjectProgress(map);

                if (projectId == null) {
                    throw new IllegalStateException("프로젝트를 찾을 수 없습니다.");
                }
            }

            int result = mapper.updateReview(map);

            if (result != 1) {
                throw new IllegalStateException(
                        "이미 처리되었거나 결재할 수 없는 보고서입니다."
                );
            }

            if (status == 1 && dto.getReportCategory() == 2) {

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
            log.error("updateReview : ", e);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

	@Override
	public Map<String, Object> countReportStatuses(Map<String, Object> map) {
		try {
			return mapper.countReportStatuses(map);
		} catch (Exception e) {
			log.info("countReportStatuses : ", e);
			throw new IllegalStateException("보고서 결재 상태 조회 실패", e);
		}
	}
}