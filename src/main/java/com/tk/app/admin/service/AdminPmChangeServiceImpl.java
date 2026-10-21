
package com.tk.app.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.tk.app.admin.domain.dto.AdminPmChangeDto;
import com.tk.app.admin.mapper.AdminPmChangeMapper;

@Service
@RequiredArgsConstructor
public class AdminPmChangeServiceImpl implements AdminPmChangeService {

	private final AdminPmChangeMapper mapper;

	@Override
	public List<AdminPmChangeDto> findAllPmChanges() {
		try {
			return mapper.findAllPmChanges();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	@Transactional
	public int processPmChange(AdminPmChangeDto dto) {
	    try {
	        AdminPmChangeDto savedDto = mapper.findPmChangeById(dto.getPm_change_id());

	       
	        if (savedDto == null) {
	            throw new IllegalStateException("존재하지 않는 요청입니다.");
	        }

	        if (savedDto.getStatus() != 0) {
	            throw new IllegalStateException("이미 처리된 요청입니다.");
	        }
	        
	        if (dto.getStatus() != 1 && dto.getStatus() != 2) {
	            throw new IllegalArgumentException("잘못된 상태입니다.");
	        }

	        
	        savedDto.setStatus(dto.getStatus());
	        savedDto.setReject_reason(dto.getReject_reason());
	        savedDto.setAdmin_id(dto.getAdmin_id());

	        int result = mapper.updatePmChangeStatus(savedDto);
	        
	        if (result != 1) {
	        	throw new IllegalStateException("요청 처리 실패");
	        }
	      
	        if (savedDto.getStatus() == 1) {
	        	
		        	if (savedDto.getOld_member_id() == savedDto.getNew_member_id()) {
		        	    throw new IllegalStateException("기존 사장과 변경할 사장은 같을 수 없습니다.");
		        	}
	        	
		        	if (mapper.countNewPmDt(savedDto) != 1) {
		        	    throw new IllegalStateException("변경할 사장은 과장 직급이어야 합니다.");
		        	}
	        	
		        	if (mapper.countNewPmMember(savedDto) != 1) {
		        	    throw new IllegalStateException("변경할 사장이 해당 워크의 회원이 아닙니다.");
		        	}
		        	
		        	// 1. 기존 PM에게 새 PM 후보가 담당하던 프로젝트 인계
		        	int projectCount = mapper.findDtProjectIds(savedDto).size();

		        	if (projectCount == 0) {
		        	    throw new IllegalStateException("인계할 과장 담당 프로젝트가 없습니다.");
		        	}

		        	if (mapper.updateOldPmProjectRole(savedDto) != projectCount) {
		        	    throw new IllegalStateException("프로젝트 인계 실패");
		        	}

		        	// 2. 기존 PM의 남은 프로젝트 PM 권한 정리
		        	mapper.deactivateOldPmProjects(savedDto);

		        	// 3. 새 PM이 참여 중인 프로젝트 권한 변경
		        	mapper.updateNewPmProjectRole(savedDto);
		        	
		        	// 4. 예전에 탈퇴한 프로젝트 다시 활성화
		        	mapper.reactivateNewPmProjects(savedDto);

		        	// 4. 새 PM이 참여하지 않은 프로젝트에 등록
		        	mapper.insertNewPmProjects(savedDto);
		        	
		        	int allCount = mapper.countAllProjects(savedDto);
		        	int newPmCount = mapper.countNewPmProjects(savedDto);

		        	if (allCount != newPmCount) {
		        	    throw new IllegalStateException("변경할 사장의 프로젝트 등록에 실패했습니다.");
		        	}
	        	
	            if (mapper.updateWorkspacePm(savedDto) != 1) {
	                throw new IllegalStateException("워크 사장 변경에 실패했습니다.");
	            }

	            if (mapper.updateOldPmRole(savedDto) != 1) {
	                throw new IllegalStateException("기존 사장의 권한 변경에 실패했습니다.");
	            }

	            if (mapper.updateNewPmRole(savedDto) != 1) {
	                throw new IllegalStateException("변경할 사장의 권한 변경에 실패했습니다.");
	            }
	        }

	        return result;

	    } catch (Exception e) {
	        e.printStackTrace();
	        throw e;
	    }
	}

}
