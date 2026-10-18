
package com.tk.app.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.mapper.MemberMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberStatusFilter extends OncePerRequestFilter {

    private final MemberMapper memberMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            String loginId = authentication.getName();

            MemberDto member = memberMapper.findByLoginId(loginId);

            if (member == null || member.getStatus() != 0) {

                SecurityContextHolder.clearContext();

                HttpSession session = request.getSession(false);

                if (session != null) {
                    session.invalidate();
                }

                response.sendRedirect(
                        request.getContextPath()
                        + "/member/login?error=restricted");

                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
