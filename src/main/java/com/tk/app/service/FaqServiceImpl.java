package com.tk.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tk.app.domain.entity.Faq;
import com.tk.app.repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FaqServiceImpl implements FaqService {
	private final FaqRepository faqRepository;
	
	@Override
	public void insertFaq(Faq faq) {
		try {
			faqRepository.save(faq);
		} catch (Exception e) {
			log.error("insertFaq : ", e);
			throw e;
		}
		
	}

	@Override
	public List<Faq> listFaq() {
		try {
			return faqRepository.findAll();
		} catch (Exception e) {
			log.error("listFaq : ", e);
			throw e;
		}
	}

	@Override
	public Faq getFaq(Long faqId) {
		try {
			return faqRepository.findById(faqId).orElse(null);
		} catch (Exception e) {
			log.error("getFaq : ", e);
			throw e;
		}
	}

}
