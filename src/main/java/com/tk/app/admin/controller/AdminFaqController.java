package com.tk.app.admin.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tk.app.domain.entity.Faq;
import com.tk.app.service.FaqService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/faq")
public class AdminFaqController {

	private final FaqService faqService;
	
	@PostMapping("/insert")
	public String insertFaq(Faq faq) {
		try {
			faqService.insertFaq(faq);
		} catch (Exception e) {
			log.error("insertFaq : ", e);
			throw e;
		}
		return "redirect:/admin/faq";
	}
	
	@GetMapping("")
	public String listFaq(Model model) {
		try {
			List<Faq> list = faqService.listFaq();
			
			model.addAttribute("faqList", list);
			model.addAttribute("activeSection", "faq");
		} catch (Exception e) {
			log.error("listFaq : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/write")
	public String writeFaq(Model model) {
		try {
			model.addAttribute("activeSection", "faqWrite");
		} catch (Exception e) {
			log.error("writeFaq : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@GetMapping("/edit")
	public String editFaq(@RequestParam("faqId") Long faqId, Model model) {
		try {
			Faq faq = faqService.getFaq(faqId);
			
			model.addAttribute("faq", faq);
			model.addAttribute("activeSection", "faqEdit");
		} catch (Exception e) {
			log.error("editFaq : ", e);
			throw e;
		}
		return "admin/admin_layout";
	}
	
	@PostMapping("/update")
	public String updateFaq(Faq faq) {
		try {
			faqService.updateFaq(faq);
		} catch (Exception e) {
			log.error("updateFaq : ", e);
			throw e;
		}
		return "redirect:/admin/faq";
	}
	
	@PostMapping("/delete")
	public String deleteFaq(@RequestParam("faqId") Long faqId) {
		try {
			faqService.deleteFaq(faqId);
		} catch (Exception e) {
			log.error("deleteFaq : ", e);
			throw e;
		}
		return "redirect:/admin/faq";
	}
}
