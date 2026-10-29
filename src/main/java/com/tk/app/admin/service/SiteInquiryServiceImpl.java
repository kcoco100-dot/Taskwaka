package com.tk.app.admin.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.admin.domain.dto.SiteInquiryDto;
import com.tk.app.admin.domain.dto.SiteInquiryFileDto;
import com.tk.app.admin.mapper.SiteInquiryMapper;
import com.tk.app.common.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteInquiryServiceImpl implements SiteInquiryService{
	private final SiteInquiryMapper siteInquiryMapper;
	private final StorageService storageService;
	
	@Value("${file.upload-root}/inquiry")
	private String uploadPath;
	
	@Transactional(rollbackFor = Exception.class)
	@Override
	public int insertInquiry(SiteInquiryDto dto, MultipartFile[] files) {
		List<String> savedFiles = new ArrayList<>();
		try {
			int result = siteInquiryMapper.insertInquiry(dto);
			
			if(files != null) {
				for(MultipartFile file : files) {
					if(file.isEmpty()) {
						continue;
					}
					String storedName = storageService.uploadFileToServer(file, uploadPath);
					savedFiles.add(storedName);
					
					SiteInquiryFileDto fileDto = new SiteInquiryFileDto();
					
					fileDto.setInquiry_id(dto.getInquiry_id());
					fileDto.setOriginal_name(file.getOriginalFilename());
					fileDto.setStored_name(storedName);
					fileDto.setFile_path(uploadPath);
					fileDto.setFile_size(file.getSize());
					siteInquiryMapper.insertInquiryFile(fileDto);
				}
			}
			
			
			return result;
		} catch (Exception e) {
			for(String storedName : savedFiles) {
		        try {
		            storageService.deleteFile(uploadPath, storedName);
		        } catch (Exception e2) {
		            log.error("첨부파일 정리 실패 : " + storedName, e2);
		        }
		    }
			
			log.error("insertInquiry : ",e);
			throw e;
		}
	}

	@Override
	public List<SiteInquiryDto> selectMyInquiryList(long member_id) {
		try {
			return siteInquiryMapper.selectMyInquiryList(member_id);
		} catch (Exception e) {
			log.error("selectMyInquiryList : ",e);
			throw e;
		}
	}

	@Override
	public SiteInquiryDto selectInquiryDetail(long inquiry_id, long member_id) {
		try {
			return siteInquiryMapper.selectInquiryDetail(inquiry_id, member_id);
		} catch (Exception e) {
			log.error("selectInquiryDetail : ",e);
			throw e;
		}
	}

	@Override
	public List<SiteInquiryDto> selectAllInquiryList() {
		try {
			return siteInquiryMapper.selectAllInquiryList();
		} catch (Exception e) {
			log.error("selectAllInquiryList : ",e);
			throw e;
		}
	}

	@Override
	public SiteInquiryDto selectAdminInquiryDetail(long inquiry_id) {
		try {
			return siteInquiryMapper.selectAdminInquiryDetail(inquiry_id);
		} catch (Exception e) {
			log.error("selectAdminInquiryDetail : ",e);
			throw e;
		}
	}

	@Override
	public int updateInquiryAnswer(SiteInquiryDto dto) {
		try {
			return siteInquiryMapper.updateInquiryAnswer(dto);
		} catch (Exception e) {
			log.error("updateInquiryAnswer : ",e);
			throw e;
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int updateInquiry(SiteInquiryDto dto, MultipartFile[] files) {
		List<String> savedFiles = new ArrayList<>();
		try {
			int result = siteInquiryMapper.updateInquiry(dto);
			
			if(result > 0 && files != null) {
			    for(MultipartFile file : files) {
			        if(file.isEmpty()) continue;
			        String storedName = storageService.uploadFileToServer(file, uploadPath);
			        savedFiles.add(storedName);
			        
			        SiteInquiryFileDto fileDto = new SiteInquiryFileDto();
			        
			        fileDto.setInquiry_id(dto.getInquiry_id());
			        fileDto.setOriginal_name(file.getOriginalFilename());
			        fileDto.setStored_name(storedName);
			        fileDto.setFile_path(uploadPath);
			        fileDto.setFile_size(file.getSize());
			        
			        siteInquiryMapper.insertInquiryFile(fileDto);
			    }
			}
			return result;
		} catch (Exception e) {
			 for(String storedName : savedFiles) {
			     try {
			         storageService.deleteFile(uploadPath, storedName);
			     } catch (Exception e2) {
			         log.error("첨부파일 정리 실패 : " + storedName, e2);
			     }
			 }
			
			log.error("updateInquiry : ",e);
			throw e;
		}
	}

	@Override
	public boolean canEditInquiry(long inquiry_id, long member_id) {
		try {
			SiteInquiryDto inquiry = siteInquiryMapper.selectInquiryDetail(inquiry_id, member_id);
			
			if(inquiry == null) {
			    return false;
			}

			return inquiry.getStatus() == 0;
			
			
		} catch (Exception e) {
			log.error("canEditInquiry : ",e);
			throw e;
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int deleteInquiry(SiteInquiryDto dto) {
	    try {
	        if(!canEditInquiry(dto.getInquiry_id(), dto.getMember_id())) {
	            return 0;
	        }

	        List<SiteInquiryFileDto> fileList = siteInquiryMapper.selectInquiryFileList(dto.getInquiry_id());

	        siteInquiryMapper.deleteInquiryFiles(dto.getInquiry_id());

	        int result = siteInquiryMapper.deleteInquiry(dto);

	        if(result == 0) {
	            throw new IllegalStateException("문의 삭제 실패");
	        }

	        for(SiteInquiryFileDto fileDto : fileList) {
	            boolean deleted = storageService.deleteFile(fileDto.getFile_path(), fileDto.getStored_name());

	            if(!deleted) {
	                throw new IllegalStateException("첨부파일 삭제 실패");
	            }
	        }

	        return result;
	    } catch (Exception e) {
	        log.error("deleteInquiry : ", e);
	        throw e;
	    }
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public int deleteAdminInquiry(SiteInquiryDto dto) {
	    try {
	        List<SiteInquiryFileDto> fileList = siteInquiryMapper.selectInquiryFileList(dto.getInquiry_id());

	        siteInquiryMapper.deleteInquiryFiles(dto.getInquiry_id());
	        
	        int result = siteInquiryMapper.deleteAdminInquiry(dto);

	        if(result == 0) {
	        		throw new IllegalStateException("문의 삭제 실패");
	        }

	        for(SiteInquiryFileDto fileDto : fileList) {
	            boolean deleted = storageService.deleteFile(fileDto.getFile_path(), fileDto.getStored_name());

	            if(!deleted) {
	                throw new IllegalStateException("첨부파일 삭제 실패");
	            }
	        }

	        return result;
	    } catch(Exception e) {
	        log.error("deleteAdminInquiry : ", e);
	        throw e;
	    }
	}

	@Override
	public List<SiteInquiryFileDto> selectInquiryFileList(long inquiry_id) {
		try {
			return siteInquiryMapper.selectInquiryFileList(inquiry_id);
		} catch (Exception e) {
			log.error("selectInquiryFileList : ",e);
			throw e;
		}
	}

	@Override
	public SiteInquiryFileDto selectInquiryFile(long inquiries_file_id) {
		try {
			return siteInquiryMapper.selectInquiryFile(inquiries_file_id);
		} catch (Exception e) {
			log.error("selectInquiryFile : ",e);
			throw e;
		}
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean deleteInquiryFile(long inquiries_file_id, long member_id) {
		try {
			SiteInquiryFileDto fileDto = siteInquiryMapper.selectInquiryFile(inquiries_file_id);
			if(fileDto == null) {
				return false;
			}
			
			boolean canEdit = canEditInquiry(fileDto.getInquiry_id(), member_id);
			if(!canEdit) {
				return false;
			}
			
			int result = siteInquiryMapper.deleteInquiryFile(inquiries_file_id);
			if(result == 0) {
				return false;
			}
			boolean deleted = storageService.deleteFile(fileDto.getFile_path(), fileDto.getStored_name());
			if(!deleted) {
				throw new IllegalStateException("첨부파일 삭제 실패");
			}
		} catch (Exception e) {
			log.error("deleteInquiryFile : ",e);
			throw e;
		}
		return true;
	}

}
