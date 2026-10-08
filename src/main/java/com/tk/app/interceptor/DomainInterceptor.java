
package com.tk.app.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.WorkspaceService;
import com.tk.app.admin.domain.dto.WorkspaceDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DomainInterceptor implements HandlerInterceptor {

    private final WorkspaceService workspaceService;

    public DomainInterceptor(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // 1. 현재 접속한 URL 가져오기
        String uri = request.getRequestURI();

        String path = uri.substring(
                request.getContextPath().length());

        // 2. 워크 주소인지 확인
        if (!path.startsWith("/") || path.length() <= 1) {
            response.sendError(404);
            return false;
        }

        // 3. URL에서 워크 도메인 키 추출
        String domainKey =
                path.substring(1).split("/")[0];

        // 4. 도메인 키 형식 검사
        if (!domainKey.matches("[a-zA-Z0-9-]+")) {
            response.sendError(403);
            return false;
        }

        // 5. 로그인 회원 정보 가져오기
        SessionInfo member =
                LoginMemberUtil.getSessionInfo();

        if (member == null) {
            response.sendRedirect(
                request.getContextPath() + "/member/login");
            return false;
        }

        // 6. 로그인 회원번호
        long memberId = member.getMember_id();

        // 7. DB에서 해당 워크 소속 확인
        WorkspaceDto workspace =
                workspaceService.findAccessibleWorkspace(
                        domainKey, memberId);

        // 8. 소속이 아니라면 차단
        if (workspace == null) {
            response.sendError(403);
            return false;
        }

        // 9. Controller에서도 사용하도록 저장
        request.setAttribute("workspace", workspace);
        request.setAttribute("domainKey", domainKey);

        // 10. 접근 허용
        return true;
    }
}
