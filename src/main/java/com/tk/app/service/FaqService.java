package com.tk.app.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.tk.app.domain.entity.Faq;

public interface FaqService {
	public void insertFaq(Faq faq);
	public List<Faq> listFaq();
	public Faq getFaq(Long faqId);
	public void updateFaq(Faq faq);
	public void deleteFaq(Long faqId);
	public Page<Faq> listPage(String category, String subcategory, String keyword, int page);
}
