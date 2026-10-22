package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.common.MyUtil;
import com.tk.app.common.PaginateUtil;
import com.tk.app.common.RequestUtils;
import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.ManageNoticeDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageNoticeService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageNoticeController {

    private final ManageNoticeService service;
    private final PaginateUtil paginateUtil;
    private final MyUtil myUtil;
    private final StorageService storageService;

    @Value("${file.upload-root}")
    private String uploadPath;

    @ModelAttribute("domainKey")
    public String setDomainKey(@PathVariable("domainKey") String domainKey) {
        return domainKey;
    }

    // 공지사항 목록 조회 및 페이징
    @GetMapping("/noticeList")
    public String noticeList(
            @PathVariable("domainKey") String domainKey,
            @RequestParam(value = "page", defaultValue = "1") int current_page,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            HttpServletRequest req,
            Model model) {

        String result = "manage/noticeList";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                result = "redirect:/login";
            } else {
                int size = 10;
                int dataCount = 0;
                int totalPage = 0;

                kwd = myUtil.decodeUrl(kwd);

                Map<String, Object> map = new HashMap<>();
                map.put("domainKey", domainKey);
                map.put("memberId", info.getMember_id());
                map.put("schType", schType);
                map.put("kwd", kwd);

                dataCount = service.dataCount(map);

                if (dataCount > 0) {
                    totalPage = paginateUtil.pageCount(dataCount, size);
                }

                current_page = totalPage > 0
                        ? Math.max(1, Math.min(current_page, totalPage))
                        : 1;

                int offset = (current_page - 1) * size;

                map.put("offset", offset);
                map.put("size", size);

                List<ManageNoticeDto> list = service.selectNoticeList(map);

                String cp = RequestUtils.getContextPath();
                String listUrl = cp + "/" + domainKey + "/manage/noticeList";

                if (!kwd.isBlank()) {
                    listUrl += "?schType=" + schType
                            + "&kwd=" + myUtil.encodeUrl(kwd);
                }

                String paging = paginateUtil.paging(current_page, totalPage, listUrl);

                model.addAttribute("list", list);
                model.addAttribute("page", current_page);
                model.addAttribute("dataCount", dataCount);
                model.addAttribute("size", size);
                model.addAttribute("totalPage", totalPage);
                model.addAttribute("paging", paging);
                model.addAttribute("schType", schType);
                model.addAttribute("kwd", kwd);
            }
        } catch (Exception e) {
            log.info("noticeList : ", e);
        }

        return result;
    }

    // 공지사항 상세 조회
    @GetMapping("/noticeDetail/{notice_id}")
    public String noticeDetail(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            Model model,
            RedirectAttributes ra) {

        String result = "manage/noticeDetail";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                kwd = myUtil.decodeUrl(kwd);

                Map<String, Object> map = createReadMap(domainKey, info, notice_id);
                map.put("schType", schType);
                map.put("kwd", kwd);

                ManageNoticeDto dto = service.selectNoticeDetail(map);

                if (dto == null) {
                    ra.addFlashAttribute("errorMessage", "공지를 조회할 수 없습니다.");
                    result = "redirect:/" + domainKey + "/manage/noticeList";
                } else {
                    service.updateViewCount(map);
                    dto = service.selectNoticeDetail(map);

                    List<ManageNoticeDto> noticeFiles = service.selectNoticeFiles(map);

                    Map<String, Object> moveMap = new HashMap<>(map);
                    moveMap.put("notice_type", dto.getNotice_type());
                    moveMap.put("workspace_id", dto.getWorkspace_id());
                    moveMap.put("project_id", dto.getProject_id());
                    moveMap.put("task_id", dto.getTask_id());

                    ManageNoticeDto prevNotice = service.selectPrevNotice(moveMap);
                    ManageNoticeDto nextNotice = service.selectNextNotice(moveMap);

                    model.addAttribute("notice", dto);
                    model.addAttribute("noticeFile", noticeFiles);
                    model.addAttribute("noticeFiles", noticeFiles);
                    model.addAttribute("prevNotice", prevNotice);
                    model.addAttribute("nextNotice", nextNotice);
                    model.addAttribute("page", page);
                    model.addAttribute("schType", schType);
                    model.addAttribute("kwd", kwd);
                }
            }
        } catch (Exception e) {
            log.info("noticeDetail : ", e);
            ra.addFlashAttribute("errorMessage", "공지 상세 조회 중 오류가 발생했습니다.");
            result = "redirect:/" + domainKey + "/manage/noticeList";
        }

        return result;
    }

    // 공지사항 등록 화면
    @GetMapping("/noticeWrite")
    public String noticeWrite(
            @PathVariable("domainKey") String domainKey,
            Model model) {

        String result = "manage/noticeWrite";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                ManageNoticeDto dto = new ManageNoticeDto();
                dto.setDomainKey(domainKey);
                dto.setMember_id(info.getMember_id());

                model.addAttribute("notice", dto);
                model.addAttribute("mode", "write");
            }
        } catch (Exception e) {
            log.info("noticeWrite : ", e);
        }

        return result;
    }

    // 공지사항 등록 및 첨부파일 업로드
    @PostMapping("/noticeWrite")
    public String createNotice(
            @PathVariable("domainKey") String domainKey,
            @ModelAttribute("notice") ManageNoticeDto dto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Model model,
            RedirectAttributes ra) {

        String result = "redirect:/" + domainKey + "/manage/noticeList";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                dto.setDomainKey(domainKey);
                dto.setMember_id(info.getMember_id());
                dto.setRequester_member_id(info.getMember_id());

                int count = service.createNotice(dto, files);

                if (count > 0) {
                    ra.addFlashAttribute("message", "공지가 등록되었습니다.");
                    result = "redirect:/" + domainKey
                            + "/manage/noticeDetail/" + dto.getNotice_id();
                } else {
                    model.addAttribute("errorMessage", "공지 등록에 실패했습니다.");
                    model.addAttribute("mode", "write");
                    result = "manage/noticeWrite";
                }
            }
        } catch (Exception e) {
            log.info("createNotice : ", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("mode", "write");
            result = "manage/noticeWrite";
        }

        return result;
    }

    // 공지사항 수정 화면
    @GetMapping("/noticeEdit/{notice_id}")
    public String noticeEdit(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            Model model,
            RedirectAttributes ra) {

        String result = "manage/noticeEdit";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                Map<String, Object> map = createReadMap(domainKey, info, notice_id);

                ManageNoticeDto dto = service.selectNoticeDetail(map);

                if (dto == null) {
                    ra.addFlashAttribute("errorMessage", "공지를 조회할 수 없습니다.");
                    result = "redirect:/" + domainKey + "/manage/noticeList";
                } else {
                    List<ManageNoticeDto> noticeFiles = service.selectNoticeFiles(map);

                    model.addAttribute("notice", dto);
                    model.addAttribute("noticeFile", noticeFiles);
                    model.addAttribute("noticeFiles", noticeFiles);
                    model.addAttribute("mode", "edit");
                }
            }
        } catch (Exception e) {
            log.info("noticeEdit : ", e);
            result = "redirect:/" + domainKey + "/manage/noticeList";
        }

        return result;
    }

    // 공지사항 수정 및 첨부파일 변경
    @PostMapping("/noticeEdit")
    public String editNotice(
            @PathVariable("domainKey") String domainKey,
            @ModelAttribute("notice") ManageNoticeDto dto,
            @RequestParam(value = "deleteFileIds", required = false) List<Long> deleteFileIds,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Model model,
            RedirectAttributes ra) {

        String result = "redirect:/" + domainKey + "/manage/noticeList";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                dto.setDomainKey(domainKey);
                dto.setMember_id(info.getMember_id());
                dto.setRequester_member_id(info.getMember_id());

                int count = service.editNotice(dto, deleteFileIds, files);

                if (count > 0) {
                    ra.addFlashAttribute("message", "공지가 수정되었습니다.");
                    result = "redirect:/" + domainKey
                            + "/manage/noticeDetail/" + dto.getNotice_id();
                } else {
                    model.addAttribute("errorMessage", "공지 수정에 실패했습니다.");
                    model.addAttribute("mode", "edit");
                    result = "manage/noticeEdit";
                }
            }
        } catch (Exception e) {
            log.info("editNotice : ", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("mode", "edit");
            result = "manage/noticeEdit";
        }

        return result;
    }

    // 공지사항 삭제 (status = 0으로 변경)
    @PostMapping("/noticeDelete/{notice_id}")
    public String deleteNotice(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            RedirectAttributes ra) {

        String result = "redirect:/" + domainKey + "/manage/noticeList";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("notice_id", notice_id);
                map.put("domainKey", domainKey);
                map.put("requester_member_id", info.getMember_id());

                int count = service.deleteNotice(map);

                if (count > 0) {
                    ra.addFlashAttribute("message", "공지가 삭제되었습니다.");
                } else {
                    ra.addFlashAttribute("errorMessage", "공지를 삭제할 수 없습니다.");
                }
            }
        } catch (Exception e) {
            log.info("deleteNotice : ", e);
            ra.addFlashAttribute("errorMessage", "공지 삭제 중 오류가 발생했습니다.");
        }

        return result;
    }

    // 첨부파일 단건 삭제
    @PostMapping("/noticeFileDelete")
    public String deleteNoticeFile(
            @PathVariable("domainKey") String domainKey,
            @RequestParam("notice_id") Long notice_id,
            @RequestParam("notice_file_id") Long notice_file_id,
            RedirectAttributes ra) {

        String result = "redirect:/" + domainKey + "/manage/noticeDetail/" + notice_id;

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = "redirect:/login";
            } else {
                Map<String, Object> map = new HashMap<>();
                map.put("notice_id", notice_id);
                map.put("notice_file_id", notice_file_id);
                map.put("domainKey", domainKey);
                map.put("memberId", info.getMember_id());
                map.put("requester_member_id", info.getMember_id());

                int count = service.deleteNoticeFile(map);

                if (count <= 0) {
                    ra.addFlashAttribute("errorMessage", "첨부파일을 삭제할 수 없습니다.");
                } else {
                    ra.addFlashAttribute("message", "첨부파일이 삭제되었습니다.");
                }
            }
        } catch (Exception e) {
            log.info("deleteNoticeFile : ", e);
            ra.addFlashAttribute("errorMessage", "첨부파일 삭제 중 오류가 발생했습니다.");
        }

        return result;
    }

    // 첨부파일 다운로드
    @GetMapping("/noticeFile/{notice_id}/{notice_file_id}/download")
    public ResponseEntity<?> downloadNoticeFile(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            @PathVariable("notice_file_id") Long notice_file_id) {

        ResponseEntity<?> result = ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();

            if (info == null) {
                result = ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                Map<String, Object> map = createReadMap(domainKey, info, notice_id);
                map.put("notice_file_id", notice_file_id);

                ManageNoticeDto file = service.selectNoticeFile(map);

                if (file != null
                        && file.getStored_name() != null
                        && !file.getStored_name().isBlank()) {

                    String filePath = file.getFile_path();

                    if (filePath == null || filePath.isBlank()) {
                        filePath = uploadPath;
                    }

                    result = storageService.downloadFile(
                            filePath,
                            file.getStored_name(),
                            file.getOriginal_name());
                }
            }
        } catch (Exception e) {
            log.info("downloadNoticeFile : ", e);
        }

        return result;
    }

    // 조회용 공통 Map 생성
    private Map<String, Object> createReadMap(String domainKey, SessionInfo info, Long notice_id) {

        Map<String, Object> map = new HashMap<>();
        map.put("domainKey", domainKey);
        map.put("memberId", info.getMember_id());

        if (notice_id != null) {
            map.put("notice_id", notice_id);
        }

        return map;
    }
}