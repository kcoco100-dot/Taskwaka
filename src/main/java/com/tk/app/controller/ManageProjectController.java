
package com.tk.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tk.app.domain.dto.ManageDto;
import com.tk.app.domain.dto.ManageTaskDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.ManageProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/{domainKey}/manage")
public class ManageProjectController {

    private final ManageProjectService servive;

    // 도메인키 공통 설정
    @ModelAttribute("domainKey")
    public String setDomainKey(
            @PathVariable("domainKey") String domainKey) {

        return domainKey;
    }

    // 로그인 사용자 정보
    private SessionInfo getLoginInfo() {

        SessionInfo info = LoginMemberUtil.getSessionInfo();

        if (info == null) {
        	throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return info;
    }

    // 프로젝트 공통
    private Map<String, Object> createMap(String domainKey, Long projectId) {

        SessionInfo info = getLoginInfo();
        Map<String, Object> map = new HashMap<>();

        map.put("domainKey", domainKey);
        map.put("projectId", projectId);
        map.put("memberId", info.getMember_id());

        return map;
    }

    // 프로젝트 목록 및 검색
    @GetMapping("/view")
    public String projectView(
            @PathVariable("domainKey") String domainKey,
            @RequestParam(value = "page", defaultValue = "1") int current_page,
            @RequestParam(value = "status", defaultValue = "all") String status,
            @RequestParam(value = "searchType", defaultValue = "all") String searchType,
            @RequestParam(value = "keyword", defaultValue = "") String keyword,
            Model model) throws Exception {

        try {
            getLoginInfo();

            int size = 10;

            status = status == null ? "all" : status.trim();
            searchType = searchType == null ? "all" : searchType.trim();
            keyword = keyword == null ? "" : keyword.trim();

            if (!status.equals("all")
                    && !status.equals("0")
                    && !status.equals("1")
                    && !status.equals("2")
                    && !status.equals("3")) {
                status = "all";
            }

            if (!searchType.equals("all")
                    && !searchType.equals("subject")
                    && !searchType.equals("content")
                    && !searchType.equals("manager")) {
                searchType = "all";
            }

            Map<String, Object> map = new HashMap<>();

            map.put("domainKey", domainKey);
            map.put("status", status);
            map.put("searchType", searchType);
            map.put("keyword", keyword);

            int dataCount = servive.dataCount(map);
            int totalPage = Math.max(1, (dataCount + size - 1) / size);

            current_page = Math.max(1, current_page);
            current_page = Math.min(current_page, totalPage);

            int offset = (current_page - 1) * size;

            map.put("offset", offset);
            map.put("size", size);

            List<ManageDto> projectList = servive.listProject(map);

            model.addAttribute("projectList", projectList);
            model.addAttribute("dataCount", dataCount);
            model.addAttribute("size", size);
            model.addAttribute("totalPage", totalPage);
            model.addAttribute("page", current_page);

            model.addAttribute("status", status);
            model.addAttribute("searchType", searchType);
            model.addAttribute("keyword", keyword);

        } catch (Exception e) {
            log.error("projectView : ", e);
            throw e;
        }

        return "manage/view";
    }

    // 프로젝트 등록 화면
    @GetMapping("/write")
    public String writeForm(Model model) {

        getLoginInfo();

        model.addAttribute("mode", "write");
        model.addAttribute("dto", new ManageDto());

        return "manage/viewWrite";
    }

    // 프로젝트 등록 처리
    @PostMapping("/write")
    public String writeSubmit(
            @PathVariable("domainKey") String domainKey,
            @ModelAttribute ManageDto dto,
            RedirectAttributes redirectAttributes)
            throws Exception {

        try {
            SessionInfo info = getLoginInfo();

            Long workspaceId = servive.findByDomainKey(domainKey);
            if (workspaceId == null) {
            	throw new ResponseStatusException(HttpStatus.NOT_FOUND, "워크스페이스가 존재하지 않습니다.");
            }

            dto.setWorkspace_id(workspaceId);
            dto.setMember_id(info.getMember_id());
            dto.setProgress(0);

            if (dto.getStatus() < 0 || dto.getStatus() > 2) {
            	throw new IllegalArgumentException("프로젝트 진행상태가 올바르지 않습니다.");
            }

            servive.insertProject(dto);
            redirectAttributes.addFlashAttribute("message", "프로젝트가 등록되었습니다.");

        } catch (Exception e) {
            log.error("writeSubmit : ", e);
            throw e;
        }

        return "redirect:/" + domainKey + "/manage/view";
    }

    // 프로젝트 상세조회
    @GetMapping("/detail/{projectId}")
    public String projectDetail(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            Model model) throws Exception {

        try {
            Map<String, Object> map = createMap(domainKey, projectId);

            ManageDto project = servive.getProjectDetail(map);

            if (project == null) {
                return "redirect:/" + domainKey + "/manage/view";
            }

            model.addAttribute("project", project);
            model.addAttribute("taskList",servive.listManageTasks(map));
            model.addAttribute("tlList",servive.listManageTaskTL(map));

            return "manage/projectDetail";

        } catch (Exception e) {
            log.error("projectDetail : ", e);
            throw e;
        }
    }

    // 프로젝트 수정 화면
    @GetMapping("/detail/{projectId}/update")
    public String updateForm(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            Model model) throws Exception {

        try {
            Map<String, Object> map = createMap(domainKey, projectId);

            ManageDto dto = servive.getProjectDetail(map);

            if (dto == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다.");
            }

            model.addAttribute("mode", "update");
            model.addAttribute("dto", dto);
            model.addAttribute("projectId", projectId);

            return "manage/viewWrite";

        } catch (Exception e) {
            log.error("updateForm : ", e);
            throw e;
        }
    }

    // 프로젝트 수정 처리
    @PostMapping("/detail/{projectId}/update")
    public String updateSubmit(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @ModelAttribute ManageDto dto,
            RedirectAttributes redirectAttributes)
            throws Exception {

        try {
            Map<String, Object> map = createMap(domainKey, projectId);

            map.put("subject", dto.getSubject());
            map.put("content", dto.getContent());
            map.put("start_date", dto.getStart_date());
            map.put("end_date", dto.getEnd_date());
            map.put("status", dto.getStatus());

            servive.updateProject(map);

            redirectAttributes.addFlashAttribute("message", "프로젝트가 수정되었습니다.");

            return "redirect:/" + domainKey + "/manage/detail/" + projectId;

        } catch (Exception e) {
            log.error("updateSubmit : ", e);
            throw e;
        }
    }

    // 프로젝트 삭제 처리
    @PostMapping("/detail/{projectId}/delete")
    public String deleteSubmit(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            RedirectAttributes redirectAttributes)
            throws Exception {

        try {
            Map<String, Object> map = createMap(domainKey, projectId);

            servive.deleteProject(map);

            redirectAttributes.addFlashAttribute("message", "프로젝트가 삭제되었습니다.");

            return "redirect:/" + domainKey + "/manage/view";

        } catch (Exception e) {
            log.error("deleteSubmit : ", e);
            throw e;
        }
    }

	// 업무 등록 처리
    @PostMapping("/detail/{projectId}/tasks/write")
    @ResponseBody
    public Map<String, Object> taskWriteSubmit(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @ModelAttribute ManageTaskDto dto) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> map = createMap(domainKey, projectId);

            dto.setProject_id(projectId);

            dto.setStatus(0);
            dto.setProgress(0.0);

            servive.insertManageTask(dto, map);

            response.put("status", "success");
            response.put("message", "업무가 등록되었습니다.");

        } catch (Exception e) {
            log.error("taskWriteSubmit : ", e);
            response.put("status", "error");
            response.put("message", "업무 등록에 실패했습니다.");
        }

        return response;
    }

    // 업무 수정 처리
    @PostMapping("/detail/{projectId}/tasks/{taskId}/update")
    @ResponseBody
    public Map<String, Object> taskUpdateSubmit(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @ModelAttribute ManageTaskDto dto) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> map = createMap(domainKey, projectId);
            map.put("taskId", taskId);

            dto.setProject_id(projectId);
            dto.setTask_id(taskId);

            servive.updateManageTask(dto, map);

            response.put("status", "success");
            response.put("message", "업무가 성공적으로 수정되었습니다.");

        } catch (Exception e) {
            log.error("taskUpdateSubmit : ", e);
            
            response.put("status", "error");
            response.put("message", "업무 수정에 실패했습니다.");
        }

        return response;
    }

    // 업무 삭제 처리
    @PostMapping("/detail/{projectId}/tasks/{taskId}/delete")
    @ResponseBody
    public Map<String, Object> taskDeleteSubmit(
            @PathVariable("domainKey") String domainKey,
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> map = createMap(domainKey, projectId);
            map.put("taskId", taskId);

            servive.deleteManageTask(map);

            response.put("status", "success");
            response.put("message", "업무가 삭제되었습니다.");

        } catch (Exception e) {
            log.error("taskDeleteSubmit : ", e);
            response.put("status", "error");
            response.put("message", "업무 삭제에 실패했습니다.");
        }

        return response; 
    }
}
