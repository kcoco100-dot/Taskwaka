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

	@Override
	public void updateFaq(Faq faq) {
		try {
			Faq oldFaq = faqRepository.findById(faq.getFaqId()).orElse(null);
			
			if(oldFaq == null) {
				throw new IllegalArgumentException("FAQ를 찾을 수 없습니다.");
			}
			
			oldFaq.setQuestion(faq.getQuestion());
			oldFaq.setAnswer(faq.getAnswer());
			oldFaq.setCategory(faq.getCategory());
			oldFaq.setSubcategory(faq.getSubcategory());
			
			faqRepository.save(oldFaq);
		} catch (Exception e) {
			log.error("updateFaq : ", e);
			throw e;
		}
		
	}

	@Override
	public void deleteFaq(Long faqId) {
		try {
			faqRepository.deleteById(faqId);
		} catch (Exception e) {
			log.error("deleteFaq : ", e);
			throw e;
		}
		
	}

}
