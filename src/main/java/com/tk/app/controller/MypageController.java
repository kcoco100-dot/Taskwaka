package com.tk.app.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
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
    private boolean isLocalAccount() {
        SessionInfo member = getLoginMember();

        try {
            return !service.isSnsMember(member.getMember_id());
        } catch (Exception e) {
            log.error("SNS 회원 여부 조회 오류 : ", e);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "회원 유형 확인 중 오류가 발생했습니다.", e
            );
        }
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
    
    @PostMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestParam(value = "selectFile", required = false) MultipartFile file) {
    	
        SessionInfo member = getLoginMember();

        try {
            String filename = service.updateProfile(member.getMember_id(), file);

            member.setAvatar(filename);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "프로필 사진이 변경되었습니다."
            ));

        } catch (Exception e) {
            log.error("프로필 사진 변경 오류 : ", e);

            HttpStatus status = e instanceof IllegalArgumentException
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            String message = status == HttpStatus.BAD_REQUEST
                    ? e.getMessage()
                    : "프로필 사진 변경에 실패했습니다.";

            return ResponseEntity.status(status).body(Map.of(
                    "success", false,
                    "message", message
            ));
        }
    }
    
    @DeleteMapping("/profile")
    public ResponseEntity<?> deleteProfile() {

        SessionInfo member = getLoginMember();

        try {
            service.deleteProfile(member.getMember_id());

            member.setAvatar(null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "프로필 사진이 삭제되었습니다."
            ));
        } catch (Exception e) {
            log.error("프로필 사진 삭제 오류 : ", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "프로필 사진 삭제에 실패했습니다."
                    ));
        }
    }

    @GetMapping("/contact")
    public String contact(Model model, 
    		@ModelAttribute(value = "isLocalAccount", binding = false) boolean isLocalAccount) throws Exception {
        SessionInfo member = getLoginMember();

        if (! isLocalAccount) {
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
    

	 @PostMapping("/contact")
	 public ResponseEntity<?> updateContact(
	         @RequestBody MypageDto dto,
	         @ModelAttribute(value = "isLocalAccount", binding = false) boolean isLocalAccount) {

	     if (! isLocalAccount) {
	         return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
	                 Map.of("success", false,
	                        "message", "접근 권한이 없습니다.")
	         );
	     }

	     SessionInfo member = getLoginMember();
	
	     try {
	         dto.setMemberId(member.getMember_id());
	
	         int result = service.updateContact(dto);
	
	         return ResponseEntity.ok(
	                 Map.of("success", result == 1,
	                        "message", "회원정보가 수정되었습니다.")
	         );
	     } catch (Exception e) {
	         log.error("마이페이지 연락처 및 주소 수정 오류 : ", e);
	         
	         HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

	         if (e instanceof IllegalArgumentException) {
	             status = HttpStatus.BAD_REQUEST;
	         } else if (e instanceof SecurityException) {
	             status = HttpStatus.FORBIDDEN;
	         } else if (e instanceof IllegalStateException) {
	             status = HttpStatus.CONFLICT;
	         }
	         
	         String message = status == HttpStatus.INTERNAL_SERVER_ERROR
	        		 ? "회원정보 수정 중 오류가 발생했습니다."
	                 : e.getMessage();
	         
	         return ResponseEntity.status(status).body(
	                 Map.of("success", false,
	                        "message", message)
	         );
	     }
	 }


    @GetMapping("/security")
    public String security(Model model, 
    		@ModelAttribute(value = "isLocalAccount", binding = false) boolean isLocalAccount) {

        if (! isLocalAccount)  {
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
