package com.tk.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tk.app.domain.entity.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long> {

}
