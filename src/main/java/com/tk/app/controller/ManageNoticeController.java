
package com.tk.app.controller;

import java.util.ArrayList;
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

    @Value("${file.upload-root}/notice")
    private String uploadPath;

    // 공통 도메인 키 전달
    @ModelAttribute("domainKey")
    public String domainKey(@PathVariable("domainKey") String domainKey) {
        return domainKey;
    }

    // 현재 역할을 HTML에 전달
    @ModelAttribute("currentRole")
    public int currentRole(@PathVariable("domainKey") String domainKey) {
        int result = 0;
        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info != null) {
                Map<String, Object> map = createMap(domainKey, info);
                result = service.selectWorkspaceRole(map);
            }
        } catch (Exception e) {
            log.info("currentRole : ", e);
        }
        return result;
    }

    // 공지사항 조회 및 검색, 페이징
    @GetMapping("/noticeList")
    public String noticeList(@PathVariable("domainKey") String domainKey,
            @RequestParam(name = "page", defaultValue = "1") int current_page,
            @RequestParam(name = "noticeType", defaultValue = "all") String noticeType,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            Model model) {

        int size = 10;
        int dataCount = 0;
        int totalPage = 0;
        List<ManageNoticeDto> list = new ArrayList<>();
        String paging = "";
        String pageBaseUrl = "";

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) return "redirect:/login";

            if (!List.of("all", "WORKSPACE", "PROJECT", "TASK").contains(noticeType)) {
                noticeType = "all";
            }
            if (!List.of("all", "title", "content").contains(schType)) {
                schType = "all";
            }

            kwd = myUtil.decodeUrl(kwd);

            Map<String, Object> map = createMap(domainKey, info);
            map.put("noticeType", noticeType);
            map.put("schType", schType);
            map.put("kwd", kwd);

            dataCount = service.dataCount(map);
            totalPage = paginateUtil.pageCount(dataCount, size);

            current_page = Math.max(1, current_page);
            if (totalPage > 0) {
                current_page = Math.min(current_page, totalPage);
            } else {
                current_page = 1;
            }

            int offset = (current_page - 1) * size;
            map.put("offset", offset);
            map.put("size", size);

            list = service.selectNoticeList(map);

            String cp = RequestUtils.getContextPath();
            String query = "noticeType=" + noticeType + "&schType=" + schType;
            if (!kwd.isBlank()) {
                query += "&kwd=" + myUtil.encodeUrl(kwd);
            }

            String listUrl = cp + "/" + domainKey + "/manage/noticeList?" + query;
            paging = paginateUtil.paging(current_page, totalPage, listUrl);
            pageBaseUrl = listUrl + "&page=";

        } catch (Exception e) {
            log.info("noticeList : ", e);
            model.addAttribute("errorMessage", "공지사항 목록을 조회하지 못했습니다.");
        }

        model.addAttribute("list", list);
        model.addAttribute("page", current_page < 1 ? 1 : current_page);
        model.addAttribute("dataCount", dataCount);
        model.addAttribute("size", size);
        model.addAttribute("totalPage", totalPage);
        model.addAttribute("paging", paging);
        model.addAttribute("pageBaseUrl", pageBaseUrl);
        model.addAttribute("noticeType", noticeType);
        model.addAttribute("schType", schType);
        model.addAttribute("kwd", kwd);

        return "manage/noticeList";
    }

    // 공지사항 상세 조회
    @GetMapping("/noticeDetail/{notice_id}")
    public String noticeDetail(@PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "noticeType", defaultValue = "all") String noticeType,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            Model model) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) return "redirect:/login";

            Map<String, Object> map = createMap(domainKey, info);
            map.put("notice_id", notice_id);

            ManageNoticeDto dto = service.selectNoticeDetail(map);
            if (dto == null) {
                return "redirect:/" + domainKey + "/manage/noticeList";
            }

            service.updateViewCount(map);
            dto = service.selectNoticeDetail(map);

            kwd = myUtil.decodeUrl(kwd);
            map.put("noticeType", noticeType);
            map.put("schType", schType);
            map.put("kwd", kwd);

            ManageNoticeDto prevDto = service.selectPrevNotice(map);
            ManageNoticeDto nextDto = service.selectNextNotice(map);

            model.addAttribute("notice", dto);
            model.addAttribute("noticeFiles", service.selectNoticeFiles(map));
            model.addAttribute("prevNotice", prevDto);
            model.addAttribute("nextNotice", nextDto);
            model.addAttribute("page", page);
            model.addAttribute("noticeType", noticeType);
            model.addAttribute("schType", schType);
            model.addAttribute("kwd", kwd);

        } catch (Exception e) {
            log.info("noticeDetail : ", e);
            return "redirect:/" + domainKey + "/manage/noticeList";
        }

        return "manage/noticeDetail";
    }

    // 공지사항 등록 화면
    @GetMapping("/noticeWrite")
    public String writeForm(@PathVariable("domainKey") String domainKey,
            Model model) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                return "redirect:/login";
            }

            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("memberId", info.getMember_id());

            model.addAttribute("mode", "write");
            model.addAttribute("notice", new ManageNoticeDto());
            model.addAttribute("projectOptions", service.selectProjectOptions(map));
            model.addAttribute("taskOptions", service.selectTaskOptions(map));

        } catch (Exception e) {
            log.info("writeForm : ", e);
            return "redirect:/" + domainKey + "/manage/noticeList";
        }

        return "manage/noticeWrite";
    }

    // 공지사항 등록 처리
    @PostMapping("/noticeWrite")
    public String writeSubmit(@PathVariable("domainKey") String domainKey,
            @ModelAttribute ManageNoticeDto dto,
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            RedirectAttributes ra) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                return "redirect:/login";
            }

            dto.setDomainKey(domainKey);
            dto.setMember_id(info.getMember_id());
            dto.setRequester_member_id(info.getMember_id());

            int result = service.createNotice(dto, files);

            if (result > 0) {
                ra.addFlashAttribute("message", "공지사항이 등록되었습니다.");
                return "redirect:/" + domainKey + "/manage/noticeDetail/" + dto.getNotice_id();
            }

            ra.addFlashAttribute("errorMessage", "공지사항 등록에 실패했습니다.");

        } catch (Exception e) {
            log.info("writeSubmit : ", e);
            ra.addFlashAttribute("errorMessage", "공지사항 등록 중 오류가 발생했습니다.");
        }

        return "redirect:/" + domainKey + "/manage/noticeWrite";
    }

    // 공지사항 수정 화면
    @GetMapping("/noticeEdit/{notice_id}")
    public String updateForm(@PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "noticeType", defaultValue = "all") String noticeType,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            Model model) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                return "redirect:/login";
            }

            Map<String, Object> map = new HashMap<>();
            map.put("domainKey", domainKey);
            map.put("memberId", info.getMember_id());
            map.put("notice_id", notice_id);

            ManageNoticeDto dto = service.selectNoticeDetail(map);
            if (dto == null) {
                return "redirect:/" + domainKey + "/manage/noticeList";
            }

            model.addAttribute("mode", "update");
            model.addAttribute("notice", dto);
            model.addAttribute("noticeFiles", service.selectNoticeFiles(map));
            model.addAttribute("page", page);
            model.addAttribute("noticeType", noticeType);
            model.addAttribute("schType", schType);
            model.addAttribute("kwd", myUtil.decodeUrl(kwd));

        } catch (Exception e) {
            log.info("updateForm : ", e);
            return "redirect:/" + domainKey + "/manage/noticeList";
        }

        return "manage/noticeWrite";
    }

    // 공지사항 수정 처리
    @PostMapping("/noticeEdit")
    public String updateSubmit(@PathVariable("domainKey") String domainKey,
            @ModelAttribute ManageNoticeDto dto,
            @RequestParam(name = "deleteFileIds", required = false) List<Long> deleteFileIds,
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "noticeType", defaultValue = "all") String noticeType,
            @RequestParam(name = "schType", defaultValue = "all") String schType,
            @RequestParam(name = "kwd", defaultValue = "") String kwd,
            RedirectAttributes ra) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                return "redirect:/login";
            }

            dto.setDomainKey(domainKey);
            dto.setRequester_member_id(info.getMember_id());

            int result = service.editNotice(dto, deleteFileIds, files);

            ra.addAttribute("page", page);
            ra.addAttribute("noticeType", noticeType);
            ra.addAttribute("schType", schType);
            ra.addAttribute("kwd", kwd);

            if (result > 0) {
                ra.addFlashAttribute("message", "공지사항이 수정되었습니다.");
                return "redirect:/" + domainKey + "/manage/noticeDetail/" + dto.getNotice_id();
            }

            ra.addFlashAttribute("errorMessage", "공지사항 수정에 실패했습니다.");

        } catch (Exception e) {
            log.info("updateSubmit : ", e);
            ra.addFlashAttribute("errorMessage", "공지사항 수정 중 오류가 발생했습니다.");
        }

        return "redirect:/" + domainKey + "/manage/noticeEdit/" + dto.getNotice_id()
                + "?page=" + page
                + "&noticeType=" + noticeType
                + "&schType=" + schType
                + "&kwd=" + myUtil.encodeUrl(kwd);
    }

    // 공지사항 논리 삭제
    @PostMapping("/noticeDelete/{notice_id}")
    public String deleteNotice(@PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            RedirectAttributes ra) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) return "redirect:/login";

            Map<String, Object> map = createMap(domainKey, info);
            map.put("requester_member_id", info.getMember_id());
            map.put("notice_id", notice_id);

            int result = service.deleteNotice(map);
            if (result > 0) {
                ra.addFlashAttribute("message", "공지사항이 삭제되었습니다.");
            } else {
                ra.addFlashAttribute("errorMessage", "공지사항을 삭제할 수 없습니다.");
            }

        } catch (Exception e) {
            log.info("deleteNotice : ", e);
            ra.addFlashAttribute("errorMessage", "공지사항 삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/" + domainKey + "/manage/noticeList";
    }

    // 공지사항 첨부파일 다운로드
    @GetMapping("/noticeFile/{notice_id}/{notice_file_id}/download")
    public ResponseEntity<?> downloadNoticeFile(@PathVariable("domainKey") String domainKey,
            @PathVariable("notice_id") Long notice_id,
            @PathVariable("notice_file_id") Long notice_file_id) {

        try {
            SessionInfo info = LoginMemberUtil.getSessionInfo();
            if (info == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Map<String, Object> map = createMap(domainKey, info);
            map.put("notice_id", notice_id);
            map.put("notice_file_id", notice_file_id);

            ManageNoticeDto file = service.selectNoticeFile(map);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            String path = file.getFile_path();
            if (path == null || path.isBlank()) {
                path = uploadPath;
            }

            return storageService.downloadFile(
                    path, file.getStored_name(), file.getOriginal_name());

        } catch (Exception e) {
            log.info("downloadNoticeFile : ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // 공통 조회 조건 생성
    private Map<String, Object> createMap(String domainKey, SessionInfo info) {
        Map<String, Object> map = new HashMap<>();
        map.put("domainKey", domainKey);
        map.put("memberId", info.getMember_id());
        return map;
    }
}
