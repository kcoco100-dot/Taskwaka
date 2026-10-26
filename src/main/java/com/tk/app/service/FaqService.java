package com.tk.app.service;

import java.util.List;

import com.tk.app.domain.entity.Faq;

public interface FaqService {
	public void insertFaq(Faq faq);
	public List<Faq> listFaq();
	public Faq getFaq(Long faqId);
	public void updateFaq(Faq faq);
	public void deleteFaq(Long faqId);
}
