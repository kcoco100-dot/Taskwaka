package com.tk.app.controller;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tk.app.domain.dto.MemberDto;
import com.tk.app.mail.MailSender;
import com.tk.app.security.CustomUserDetails;
import com.tk.app.service.MemberService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/")
public class MemberController {
	private final MemberService memberService;
	private final MailSender mailSender;

	
	@GetMapping
	public String index() {
		return "redirect:/member/login";
	}

	@GetMapping({ "member/login" })
	public String handleHome(@RequestParam(name = "error", required = false) String error, Model model) {

		if ("locked".equals(error)) {
		    model.addAttribute("message", "비밀번호를 5회 이상 잘못 입력하여 계정이 잠겼습니다.");

		} else if ("restricted".equals(error)) {
		    model.addAttribute("message", "현재 이용이 제한된 계정입니다. 관리자에게 문의해 주세요.");
		    
		} else if ("disabled".equals(error)) {
			model.addAttribute("message", "비활성화된 계정입니다. 관리자에게 문의해 주세요.");

		} else if (error != null) {
		    model.addAttribute("message", "아이디 또는 패스워드가 일치하지 않습니다.");
		}

		return "member/login-full";
	}

	@GetMapping("/main")
	public String mainHome(
	        @RequestParam(value = "inviteKey", required = false) String inviteKey,
	        @AuthenticationPrincipal CustomUserDetails user,
	        Model model) {
		if (inviteKey != null) return "redirect:" + InviteAcceptanceController.previewPath(inviteKey);
        return "main/mainHome";
	}

	/**
	 * 3. 회원가입 화면
	 */
	@GetMapping("member/register")
	public String registerForm() {
		return "member/register";
	}

	@PostMapping("/member/register")
	@ResponseBody
	public String registerSubmit(MemberDto dto, HttpSession session) {

		Boolean emailVerified = (Boolean) session.getAttribute("emailVerified");
		if (!Boolean.TRUE.equals(emailVerified)) {
			return "EMAIL_NOT_VERIFIED";
		}
		String verifiedEmail = (String) session.getAttribute("emailVerifyAddress");
		if (dto.getEmail() == null || verifiedEmail == null || !verifiedEmail.equals(dto.getEmail().trim())) {
			return "EMAIL_MISMATCH";
		}
		if (dto.getLogin_id() == null || dto.getLogin_id().isBlank()) {
			return "INVALID_ID";
		}
		if (!dto.getLogin_id().matches("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{4,16}$")) {
			return "INVALID_ID";
		}
		int count = memberService.countByLoginId(dto.getLogin_id());
		if (count > 0) {
			return "DUPLICATE_ID";
		}
		if (dto.getPassword() == null || !dto.getPassword()
				.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,20}$")) {
			return "INVALID_PASSWORD";
		}
		if (dto.getName() == null || dto.getName().isBlank()) {
			return "INVALID_NAME";
		}
		if (dto.getBirth() == null || dto.getBirth().isBlank()) {
			return "INVALID_BIRTH";
		}
		try {
			LocalDate birthDate = LocalDate.parse(dto.getBirth());

			if (birthDate.isAfter(LocalDate.now())) {
				return "INVALID_BIRTH";
			}
		} catch (DateTimeParseException e) {
			return "INVALID_BIRTH";
		}
		String phone = dto.getPhone();

		if (phone == null || !phone.matches("^0[0-9]{1,2}-[0-9]{3,4}-[0-9]{4}$")) {
			return "INVALID_PHONE";
		}

		try {
			memberService.insertMember(dto);

			session.removeAttribute("emailVerified");
			session.removeAttribute("emailVerifyAddress");

			return "REGISTER_SUCCESS";

		} catch (Exception e) {
			e.printStackTrace();
			return "REGISTER_FAIL";
		}

	}

	// 이메일 인증번호 발송
	@PostMapping("member/email/send")
	@ResponseBody
	public String sendEmailCode(@RequestParam("email") String email, HttpSession session) {

		session.setAttribute("emailVerified", false);

		// 이메일 형식 확인
		if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			return "INVALID_EMAIL";
		}

		long now = System.currentTimeMillis();

		// 인증번호 재발송은 60초 후 가능
		Long lastSent = (Long) session.getAttribute("emailSentAt");
		if (lastSent != null && now - lastSent < 60000) {
			return "WAIT";
		}

		// 6자리 인증번호 생성
		SecureRandom random = new SecureRandom();
		String code = String.valueOf(100000 + random.nextInt(900000));

		// 실제 이메일 발송
		boolean result = mailSender.sendVerificationMail(email, code);

		if (!result) {
			return "FAIL";
		}

		// 인증번호를 서버에 5분 동안 유효하도록 저장
		session.setAttribute("emailVerifyCode", code);
		session.setAttribute("emailVerifyAddress", email);
		session.setAttribute("emailVerifyExpiresAt", now + 300000);
		session.setAttribute("emailSentAt", now);
		session.setAttribute("emailVerified", false);
		session.setAttribute("emailVerifyFailCount", 0);

		return "OK";
	}

	@GetMapping("member/check-id")
	@ResponseBody
	public Integer checkLoginId(@RequestParam("login_id") String login_id) {

		return memberService.countByLoginId(login_id);
	}

	@GetMapping("/member/registerComplete")
	public String registerComplete(Model model) {
		model.addAttribute("title", "회원가입 완료");
		model.addAttribute("message", "Taskwaka 회원가입이 완료되었습니다. 로그인하여 서비스를 이용해 주세요.");

		return "member/registerComplete";
	}

	@GetMapping("member/updatePwd")
	public String updatePwdForm() throws Exception {
		return "member/updatePwd";
	}
	
	@GetMapping("member/idFind")
	public String idFindForm() {
	    return "member/idFind";
	}
	
	@PostMapping("member/idFind")
	@ResponseBody
	public String findLoginId(@RequestParam("name") String name, @RequestParam("email") String email,
			HttpSession session) {

		if (name.isBlank() || email.isBlank()) {
			return "INVALID_INPUT";
		}

		 String searchKey = email.trim().toLowerCase();

		 String countKey = "idFindCount_" + searchKey;
		 String startedKey = "idFindStartedAt_" + searchKey;

		 long now = System.currentTimeMillis();

		 Long startedAt = (Long) session.getAttribute(startedKey);
		 Integer count = (Integer) session.getAttribute(countKey);

		 if (startedAt == null || count == null || now - startedAt >= 600000) {
		     session.setAttribute(startedKey, now);
		     count = 0;
		 }

		 if (count >= 5) {
		     return "TOO_MANY";
		 }

		 session.setAttribute(countKey, count + 1);

		Map<String, Object> map = Map.of("name", name.trim(), "email", email.trim());

		List<MemberDto> members = memberService.listFindMember(map);

		if (members == null || members.isEmpty()) {
			return "NOT_FOUND";
		}

		String loginId = members.get(0).getLogin_id();

		if (loginId == null || loginId.length() < 4) {
			return "NOT_FOUND";
		}

		int visible = loginId.length() >= 6 ? 2 : 1;

		String maskedId = loginId.substring(0, visible) + "*".repeat(loginId.length() - visible * 2)
				+ loginId.substring(loginId.length() - visible);

		return "FOUND:" + maskedId;
	}

	

	@GetMapping("member/reset-password")
	public String resetPasswordForm(
	        @RequestParam("token") String token,
	        Model model) {
	
	    if (token.isBlank()) {
	        return "redirect:/member/pwdFind";
	    }
	
	    try {
	        byte[] hashBytes = MessageDigest.getInstance("SHA-256")
	                .digest(token.getBytes(StandardCharsets.UTF_8));
	
	        String tokenHash = HexFormat.of().formatHex(hashBytes);
	
	        Long memberId = memberService.findResetMemberId(
	                Map.of("token_hash", tokenHash));

			if (memberId == null) {
			    return "redirect:/member/pwdFind?expired=1";
			}
	
	        model.addAttribute("token", token);
	        return "member/resetPassword";
	
	    } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException(e);
	    }
	}
	

	@PostMapping("member/reset-password")
	public String resetPasswordSubmit(
	        @RequestParam("token") String token,
	        @RequestParam("newPassword") String newPassword,
	        @RequestParam("confirmPassword") String confirmPassword,
	        Model model) {
	
	    if (token.isBlank()) {
	        return "redirect:/member/pwdFind";
	    }
	
		if (!newPassword.equals(confirmPassword)) {
		    model.addAttribute("token", token);
		    model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
		    return "member/resetPassword";
		}
	
		if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,20}$")) {
		    model.addAttribute("token", token);
		    model.addAttribute("errorMessage", "영문 대소문자, 숫자, 특수문자를 포함해 8~20자로 입력해 주세요.");
		    return "member/resetPassword";
		}
	
	    try {
	        byte[] hashBytes = MessageDigest.getInstance("SHA-256")
	                .digest(token.getBytes(StandardCharsets.UTF_8));
	
	        String tokenHash = HexFormat.of().formatHex(hashBytes);
	
	        boolean success = memberService.resetPassword(tokenHash, newPassword);
	
			if (!success) {
			    return "redirect:/member/pwdFind?expired=1";
			}
	
	        return "redirect:/member/login?reset=success";
	
	    } catch (IllegalArgumentException e) {
	        model.addAttribute("token", token);
	        model.addAttribute("errorMessage", e.getMessage());
	        return "member/resetPassword";

	    } catch (NoSuchAlgorithmException e) {
	        throw new IllegalStateException(e);
	    }
	}

	
	@GetMapping("member/pwdFind")
	public String pwdFindForm() {
	    return "member/passwordFind";
	}

	
	@PostMapping("member/pwdFind")
	@ResponseBody
	public String pwdFindSubmit(
	        @RequestParam("login_id") String loginId,
	        @RequestParam("name") String name,
	        @RequestParam("email") String email,
	        HttpSession session) {
	
	    if (loginId.isBlank() || name.isBlank() || email.isBlank()) {
	        return "INVALID_INPUT";
	    }
	    
	    long now = System.currentTimeMillis();

	    Long startedAt = (Long) session.getAttribute("pwdFindStartedAt");
	    Integer count = (Integer) session.getAttribute("pwdFindCount");

	    if (startedAt == null || count == null || now - startedAt >= 600000) {
	        session.setAttribute("pwdFindStartedAt", now);
	        count = 0;
	    }

	    if (count >= 5) {
	        return "TOO_MANY";
	    }

	    session.setAttribute("pwdFindCount", count + 1);
	    
	    Map<String, Object> map = Map.of("login_id", loginId.trim(),
	    									"name" , name.trim(),
	    									"email", email.trim());
	    
	    MemberDto member = memberService.findByLoginIdAndEmail(map);
	    
	    if (member == null) {
	        return "NOT_FOUND";
	    }
	    
    	byte[] randomBytes = new byte[32];
    	new SecureRandom().nextBytes(randomBytes);
    	
    	String token = Base64.getUrlEncoder()
    			.withoutPadding()
    			.encodeToString(randomBytes);
    	
    	String tokenHash;
    	
    	try {
			byte[] hashBytes = MessageDigest.getInstance("SHA-256")
					.digest(token.getBytes(StandardCharsets.UTF_8));
			
			tokenHash = HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			return "FAIL";
		}
    	
    	Map<String, Object> tokenMap = Map.of(
    			"token_hash", tokenHash,
    			"member_id", member.getMember_id()
    	);
    		
    	int result = memberService.insertPasswordResetToken(tokenMap);
    	
    	if(result != 1) {
    		return "FAIL";
    	}

		String resetUrl = ServletUriComponentsBuilder
		        .fromCurrentContextPath()
		        .path("/member/reset-password")
		        .queryParam("token", token)
		        .toUriString();
		
		boolean sent;

		try {
		    sent = mailSender.sendPasswordResetMail(email.trim(), resetUrl);
		} catch (Exception e) {
		    log.error("비밀번호 재설정 이메일 발송 오류", e);
		    sent = false;
		}
		
		if (!sent) {
		    memberService.useResetToken(Map.of("token_hash", tokenHash));
		    return "FAIL";
		}

	
	    return "OK";
	}


	@GetMapping("member/expired")
	public String expired() throws Exception {
		// 세션이 익스파이어드(만료) 된 경우
		return "member/expired";
	}

	@PostMapping("/member/email/verify")
	@ResponseBody
	public String verifyEmailCode(@RequestParam("email") String email, @RequestParam("code") String code,
			HttpSession session) {

		String savedCode = (String) session.getAttribute("emailVerifyCode");
		String savedEmail = (String) session.getAttribute("emailVerifyAddress");
		Long expiresAt = (Long) session.getAttribute("emailVerifyExpiresAt");
		Integer failCount = (Integer) session.getAttribute("emailVerifyFailCount");

		if (savedCode == null || savedEmail == null || expiresAt == null) {
			return "NOT_SENT";
		}

		if (System.currentTimeMillis() > expiresAt) {
			session.removeAttribute("emailVerifyCode");
			session.setAttribute("emailVerified", false);
			return "EXPIRED";
		}

		if (!savedEmail.equals(email.trim())) {
			return "EMAIL_CHANGED";
		}

		if (failCount != null && failCount >= 5) {
			return "TOO_MANY";
		}

		if (!code.matches("\\d{6}") || !savedCode.equals(code)) {
			int nextCount = (failCount == null ? 0 : failCount) + 1;
			session.setAttribute("emailVerifyFailCount", nextCount);
			return "WRONG";
		}

		session.setAttribute("emailVerified", true);
		session.removeAttribute("emailVerifyCode");
		return "OK";
	}
}
