package com.tk.app.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.common.MyUtil;
import com.tk.app.domain.dto.ManageDto;
import com.tk.app.mapper.ManageMapper;
import com.tk.app.repository.ProjectMemberRepository;
import com.tk.app.repository.ProjectTeamView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageServiceImpl implements ManageService{
	private final ManageMapper mapper;
	private final MyUtil myUtil;
	
	@Autowired
    private ProjectMemberRepository projectMemberRepository;
	
	@Override
	public Long findByDomainKey(String domainKey) throws Exception {
		try {
            return mapper.findByDomainKey(domainKey);
        } catch (Exception e) {
            log.info("findByDomainKey : ", e);
            throw e;
        }
	}
	
	@Override
	public void insertProject(ManageDto dto) throws SQLException {
		try {
			mapper.insertProject(dto);
		} catch (Exception e) {
			log.info("insertProject : ", e);
			
			throw e;
		}
	}

	@Override
	public void updateProject(ManageDto dto) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteProject(ManageDto dto) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<ManageDto> listProject(Map<String, Object> map) throws SQLException {
		List<ManageDto> list = null;
		
		try {
			list = mapper.listProject(map);
			
		} catch (Exception e) {
			log.info("listProject : ", e);
			
			throw e;
		}
		
		return list;
	}

	@Override
	public int dataCount(Map<String, Object> map) throws Exception {
		try {
			return mapper.dataCount(map);
        } catch (Exception e) {
        	log.info("dataCount : ", e);

        	throw e;
        }
	}

	@Override
	public List<ProjectTeamView> listWorkspaceMembers(Long workspaceId) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	@Transactional
	public void assignOrUpdateProjectMember(Long workspaceMemberId, Long projectId, int role) throws Exception {
		projectMemberRepository.upsertProjectMember(workspaceMemberId, projectId, role);
	}

	@Override
	public int getProjectCount(Long workspaceId) throws Exception {
		
		return mapper.getProjectCount(workspaceId);
	}

	@Override
	public List<ManageDto> getProjectDetail(Map<String, Object> map) throws Exception {
		List<ManageDto> list = null;
		try {
			list = mapper.getProjectDetail(map);
		} catch (Exception e) {
			log.info("getProjectDetail : ", e);
		}
		
		return list;
	}
}
