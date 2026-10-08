package com.tk.app.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping
    public String adminMain() {
        return "redirect:/admin/user";
    }

    @GetMapping("/user")
    public String user(Model model) {
        return adminPage("user", model);
    }

    @GetMapping("/domain")
    public String domain(Model model) {
        return adminPage("domain", model);
    }

    @GetMapping("/notice")
    public String notice(Model model) {
        return adminPage("notice", model);
    }

    @GetMapping("/inquiry")
    public String inquiry(Model model) {
        return adminPage("inquiry", model);
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        return adminPage("stats", model);
    }

    @GetMapping("/security")
    public String security(Model model) {
        return adminPage("security", model);
    }

    private String adminPage(String section, Model model) {

        model.addAttribute("activeSection", section);
        model.addAttribute("adminName", "관리자 (ADMIN)");

        return "admin/admin_layout";
    }
}