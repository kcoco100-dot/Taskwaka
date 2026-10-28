package com.tk.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.tk.app.domain.dto.MypageDto;
import com.tk.app.domain.dto.SessionInfo;
import com.tk.app.security.LoginMemberUtil;
import com.tk.app.service.MypageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/member/mypage")
public class MypageController {

    private final MypageService service;


    private SessionInfo getLoginMember() {
        SessionInfo member = LoginMemberUtil.getSessionInfo();

        if (member == null || member.getMember_id() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."
            );
        }
        return member;
    }

    @ModelAttribute("isLocalAccount")
    public boolean isLocalAccount() {
        SessionInfo member = getLoginMember();

        return "local".equalsIgnoreCase(member.getLogin_type());
    }

    @GetMapping
    public String overview(Model model) throws Exception {
        SessionInfo member = getLoginMember();

        MypageDto dto = service.findMyInfo(member.getMember_id());

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "회원정보를 찾을 수 없습니다."
            );
        }

        model.addAttribute("page", "overview");
        model.addAttribute("myInfo", dto);

        return "mypage/mypage";
    }


    @GetMapping("/profile")
    public String profile(Model model) throws Exception {
        SessionInfo member = getLoginMember();

        MypageDto dto = service.findMyInfo(member.getMember_id());

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "회원정보를 찾을 수 없습니다."
            );
        }

        model.addAttribute("page", "profile");
        model.addAttribute("myInfo", dto);

        return "mypage/mypage";
    }


    @GetMapping("/contact")
    public String contact(Model model) throws Exception {
        SessionInfo member = getLoginMember();

        if (! "local".equalsIgnoreCase(member.getLogin_type())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,"접근 권한이 없습니다."
            );
        }

        MypageDto dto = service.findMyInfo(member.getMember_id());

        if (dto == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "회원정보를 찾을 수 없습니다."
            );
        }

        model.addAttribute("page", "contact");
        model.addAttribute("myInfo", dto);

        return "mypage/mypage";
    }


    @GetMapping("/security")
    public String security(Model model) {
        SessionInfo member = getLoginMember();

        if ( !"local".equalsIgnoreCase(member.getLogin_type())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "접근 권한이 없습니다."
            );
        }

        model.addAttribute("page", "security");

        return "mypage/mypage";
    }


    @GetMapping("/work-leave")
    public String workLeave(Model model) {

        model.addAttribute("page", "work-leave");

        return "mypage/mypage";
    }


    @GetMapping("/withdraw")
    public String withdraw(Model model) {

        model.addAttribute("page", "withdraw");

        return "mypage/mypage";
    }
}
