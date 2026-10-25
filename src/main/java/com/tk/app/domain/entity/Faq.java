package com.tk.app.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "faq")
@Getter
@Setter
@NoArgsConstructor
public class Faq {
	
	@Id
	@Column(name = "faq_id", columnDefinition = "NUMBER(19)")
	@SequenceGenerator(name = "seq_faq", sequenceName = "seq_faq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_faq")
	private Long faqId;
	
	@Column(name = "question", nullable = false, length = 300)
	private String question;
	
	@Column(name = "answer", nullable = false, columnDefinition = "CLOB")
	private String answer;
	
	@Column(name = "category", length = 100)
	private String category;
	
	@Column(name = "subcategory", length = 100)
	private String subcategory;
}
