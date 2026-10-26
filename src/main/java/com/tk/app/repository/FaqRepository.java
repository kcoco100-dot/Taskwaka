package com.tk.app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tk.app.domain.entity.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    public Page<Faq> findByQuestionContaining(String keyword, Pageable pageable);

    public Page<Faq> findByCategoryAndQuestionContaining(String category, String keyword, Pageable pageable);

    public Page<Faq> findByCategoryAndSubcategoryAndQuestionContaining(String category, String subcategory, String keyword, Pageable pageable);
}

