package com.tk.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    /**
     * 루트 경로 접속 시 home 화면으로 이동
     */
//    @GetMapping("/")
//    public String index() {
//        return "redirect:/home";
//    }

    /**
     * 1. 팀 선택 및 새로운 워크 생성 Home 화면
     */
//    @GetMapping("/home")
//    public String home(Model model) {
//        model.addAttribute("userName", "박혜림");
//        return "home/main_home";
//    }

    /**
     * 2. 로그인 화면
     */
    @GetMapping("/login")
    public String loginForm() {
        return "auth/login";
    }

    /**
     * 3. 회원가입 화면
     */
    @GetMapping("/register")
    public String registerForm() {
        return "auth/register";
    }

    /**
     * 4. 아이디 / 비밀번호 찾기 화면
     */
    @GetMapping("/find-account")
    public String findAccountForm(@RequestParam(name = "type", defaultValue = "id") String type, Model model) {
        model.addAttribute("findType", type);
        return "auth/find_account";
    }
    
    // 새 워크 생성
    @GetMapping("/create")
    public String workCreate() {
        return "work/create";
    }

    /**
     * 5. 프로젝트 화면 라우팅 (피드, 업무, 간트, 캘린더, 파일)
     */
    @GetMapping("/project")
    public String projectDetail(
            @RequestParam(name = "tab", defaultValue = "feed") String tab,
            Model model) {
        
        if (!tab.matches("feed|task|gantt|calendar|file")) {
            tab = "feed";
        }
        
        model.addAttribute("activeTab", tab);
        model.addAttribute("projectName", "Taskwaka 시작가이드");
        
        return "project/project_layout";
    }
}
