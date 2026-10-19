package com.tk.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.WorkCreateDto;
import com.tk.app.mapper.WorkCreateMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkCreateServiceImpl implements WorkCreateService {
	private final WorkCreateMapper mapper;
	private final GrowthTreeService growthTreeService;
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long insertWork(WorkCreateDto dto, String loginId) throws Exception {

		if(dto == null || loginId == null) {
			throw new IllegalArgumentException ("잘못된 요청입니다.");
		}
		
		String workSubject = dto.getWorkSubject();
		String workContent = dto.getWorkContent();
		String domainKey = dto.getDomainKey();
		
		if(workSubject == null || workSubject.isBlank()) {
			throw new IllegalArgumentException ("워크명을 입력해주세요.");
		}


		if (!workSubject.matches(
		        "(?U)(?!.*[ㄱ-ㅎㅏ-ㅣᄀ-ᇿ])\\S+( \\S+)*")) {
		
		    throw new IllegalArgumentException(
		        "워크명에는 완성되지 않은 한글 자음·모음을 사용할 수 없으며, 앞뒤 공백과 연속 공백도 허용되지 않습니다."
		    );
		}

        
        if(workSubject.length() > 150) {
        	throw new IllegalArgumentException("워크명은 150자를 넘길 수 없습니다.");
        }
        
        if(workContent != null && workContent.length() > 1000) {
        	throw new IllegalArgumentException("소개는 1000자 이하로 입력해주세요.");
        }
		
        if(domainKey == null || domainKey.isBlank()) {
        	throw new IllegalArgumentException("도메인 주소를 입력해 주세요.");
        }
        
        if(domainKey.length() > 255 ||  !domainKey.matches("[a-z0-9]+") ) {
        	throw new IllegalArgumentException("도메인은 영어 소문자, 숫자만 사용할 수 있습니다.");
        }
		
        try {
        	Long loginMemberId = mapper.findMemberIdByLoginId(loginId);
        	int count = mapper.countByDomainKey(domainKey);
        	
        	if(loginMemberId == null || loginMemberId <= 0) {
                throw new IllegalStateException("로그인 회원 정보를 찾을 수 없습니다.");
        	}
        	
        	if(count > 0) {
        		throw new IllegalArgumentException("이미 사용중인 도메인 주소입니다.");
        	}
			
        	dto.setPmMemberId(loginMemberId);
        	
        	mapper.insertWork(dto);
        	
        	if(dto.getWorkId() == null) {
        		throw new IllegalArgumentException("워크 번호 생성에 실패했습니다.");
        	}
        	
        	mapper.insertWorkPM(dto);

        	mapper.insertDomainKey(dto);
        	growthTreeService.growthTreeCreate(dto.getWorkId());

        	
        	return dto.getWorkId();
        
		} catch (Exception e) {
			log.error("insertWork : ", e);
			
			throw e;
		}
		
	}

	@Override
	public boolean isDomainKeyDuplicate(String domainKey) {
		return mapper.countByDomainKey(domainKey) > 0;
	}

}
