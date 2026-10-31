package com.tk.app.controller;

import com.tk.app.common.RequestUtils;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.ProjectActor;
import com.tk.app.mapper.TaskMapper;
import com.tk.app.security.LoginMemberUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@ControllerAdvice(assignableTypes = {ProjectController.class, TaskApiController.class})
@RequiredArgsConstructor
public class ProjectAccessAdvice {
    private final TaskMapper tasks;

    @ModelAttribute
    public void checkProject(@PathVariable Map<String, String> path, HttpServletRequest request) {
        var member = LoginMemberUtil.getSessionInfo();
        if (member == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        Map<String, Object> scope = new HashMap<>(path);
        scope.put("memberId", member.getMember_id());
        scope.put("write", !request.getMethod().equals("GET") && !request.getMethod().equals("HEAD"));
        String uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        scope.put("dailyList", uri.endsWith("/daily-reports"));
        scope.put("teamReport", uri.endsWith("/reports"));
        scope.put("createSub", uri.endsWith("/create-sub"));
        scope.put("deleteSub", path.containsKey("subTaskId") && request.getMethod().equals("DELETE"));
        ProjectActor actor = tasks.findProjectActor(scope);
        if (actor == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "프로젝트 또는 대상에 접근할 수 없습니다.");
        request.setAttribute("projectActor", actor);
        request.setAttribute("projectScope", scope);
    }
    @SuppressWarnings("unchecked")
    public void lockMutation() {
        var request = RequestUtils.getCurrentRequest();
        var scope = (Map<String,Object>) request.getAttribute("projectScope");
        if (scope == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        scope.put("lock", true);
        ProjectActor actor = tasks.findProjectActor(scope);
        if (actor == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        // 호출 전에 바인딩된 역할과 달라졌으면 새 요청에서 권한을 다시 판정한다.
        if (!actor.equals(request.getAttribute("projectActor"))) throw new ResponseStatusException(HttpStatus.CONFLICT);
    }
}
