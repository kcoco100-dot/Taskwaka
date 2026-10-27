
package com.tk.app.oauth;

import java.security.SecureRandom;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.service.LoginHistoryService;
import com.tk.app.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class KakaoController {

    private final KakaoAuthService kakaoAuthService;
    private final MemberService memberService;
    private final UserDetailsService userDetailsService;
    private final LoginHistoryService loginHistoryService;

    @Value("${kakao.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @GetMapping("/oauth/kakao")
    public String kakaoLogin(HttpSession session) {

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);

        String state = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        session.setAttribute("KAKAO_OAUTH_STATE", state);

        String url = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", restApiKey)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                //.queryParam("prompt", "login") // 카카오 로그인 나중에 풀어놓기(로그인창으로 계속 가짐)
                .build()
                .encode()
                .toUriString();

        return "redirect:" + url;
    }

    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<String> kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {

        String savedState =
                (String) session.getAttribute("KAKAO_OAUTH_STATE");

        session.removeAttribute("KAKAO_OAUTH_STATE");

        if (savedState == null || !Objects.equals(savedState, state)) {
            return ResponseEntity.badRequest()
                    .body("잘못된 카카오 로그인 요청입니다.");
        }

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body("카카오 인증 코드가 없습니다.");
        }

        try {
            String accessToken = kakaoAuthService.getAccessToken(code);

            KakaoUser kakaoUser =
                    kakaoAuthService.getUserInfo(accessToken);

            if (kakaoUser == null || kakaoUser.getId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("카카오 사용자 정보를 가져오지 못했습니다.");
            }
            
            
            
            Map<String, Object> map = new HashMap<>();
            
            map.put("provider", "KAKAO");
            map.put("provider_user_id", kakaoUser.getId().toString());
            
            MemberDto member = memberService.loginSnsMember(map);
            
            if (member == null && !kakaoUser.isEmail_verified()) {
                return ResponseEntity.badRequest()
                        .body("카카오 이메일 인증을 확인할 수 없습니다.");
            }
            
            if (member == null) {

                String year = kakaoUser.getBirthyear();
                String birthday = kakaoUser.getBirthday();

                if (isMissing(kakaoUser.getName()) ||
                	    isMissing(kakaoUser.getEmail()) ||
                	    isMissing(kakaoUser.getPhoneNumber()) ||
                	    isMissing(year) ||
                	    isMissing(birthday)) {

                	    return ResponseEntity.badRequest()
                	            .body("카카오 회원가입 필수 정보가 없습니다.");
                	}

                if (!year.matches("\\d{4}") ||
                    !birthday.matches("\\d{4}")) {
                    return ResponseEntity.badRequest()
                            .body("생년월일 형식이 올바르지 않습니다.");
                }

                String birth;

                try {
                    birth = LocalDate.of(
                            Integer.parseInt(year),
                            Integer.parseInt(birthday.substring(0, 2)),
                            Integer.parseInt(birthday.substring(2, 4))
                    ).toString();
                } catch (DateTimeException e) {
                    return ResponseEntity.badRequest()
                            .body("생년월일이 올바르지 않습니다.");
                }

                String phone = kakaoUser.getPhoneNumber()
                        .replaceAll("[^0-9]", "");

                if (phone.startsWith("82")) {
                    phone = "0" + phone.substring(2);
                }
                
                if (!phone.matches("01[016789][0-9]{7,8}")) {
                    return ResponseEntity.badRequest()
                            .body("전화번호 형식이 올바르지 않습니다.");
                }

                MemberDto dto = new MemberDto();

                dto.setSns_provider("KAKAO");
                dto.setSns_id(kakaoUser.getId().toString());
                dto.setName(kakaoUser.getName());
                dto.setEmail(kakaoUser.getEmail());
                dto.setBirth(birth);
                dto.setPhone(phone);

                memberService.insertSnsMember(dto);
                member = dto;
            }
            
            if (member.getFail_cnt() >= 5) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION,
                                request.getContextPath() + "/member/login?error=locked")
                        .build();
            }

            if (member.getStatus() != 0 ||
                    member.getBlocked_until() != null) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION,
                                request.getContextPath() + "/member/login?error=restricted")
                        .build();
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(member.getLogin_id());

            new AccountStatusUserDetailsChecker().check(userDetails);

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            memberService.updateLastLogin(member.getLogin_id());

            Long member_id = memberService.getMemberId(member.getLogin_id());
            
            loginHistoryService.insertLoginHistory(member_id);
            
            request.changeSessionId();

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            new HttpSessionSecurityContextRepository()
                    .saveContext(context, request, response);

            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION,
                            request.getContextPath() + "/main")
                    .build();


        } catch (Exception e) {
        	e.printStackTrace();
        	
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("카카오 인증 처리 중 오류가 발생했습니다.");
        }
    }
    
    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }
}
