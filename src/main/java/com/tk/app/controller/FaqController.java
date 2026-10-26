package com.tk.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tk.app.domain.entity.Faq;
import com.tk.app.service.FaqService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/faq")
public class FaqController {
	private final FaqService faqService;
	
	@GetMapping("/list")
	@ResponseBody
	public List<Faq> listFaq() {
		return faqService.listFaq();
	}
}
