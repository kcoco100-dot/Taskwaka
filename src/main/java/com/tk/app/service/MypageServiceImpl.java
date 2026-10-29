package com.tk.app.service;

import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.MypageDto;
import com.tk.app.domain.dto.MypagePwdDto;
import com.tk.app.domain.dto.MypageWorkDto;
import com.tk.app.mapper.MypageMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MypageServiceImpl implements MypageService {
	
	private final MypageMapper mapper;
	private final StorageService storageService;
	private final PasswordEncoder passwordEncoder;

	@Value("${file.upload-root}/member")
	private String uploadPath;
	
    private void deleteProfileFile(String filename) {
	    try {
	        if (filename == null || ! filename.matches("[A-Za-z0-9_-]+\\.(?i:jpg|jpeg|png)")) {
	            return;
	        }
	        
	        storageService.deleteFile(uploadPath, filename);
	        
	    } catch (Exception e) {
	        log.warn("프로필 사진 파일 삭제 오류: {}", filename, e);
	    }
    }
	
	
	@Override
	public MypageDto findMyInfo(Long memberId) throws Exception {
		return mapper.findMyInfo(memberId);
	}


    @Override
    public boolean isSnsMember(Long memberId) throws Exception {
    	boolean result = mapper.countSnsAccount(memberId) > 0;
    	
        return result;
	}
    
    @Override
    @Transactional
    public int updateContact(MypageDto dto) throws Exception {
        if (dto == null || dto.getMemberId() == null) {
            throw new IllegalArgumentException( "회원정보가 올바르지 않습니다.");
        }

        if (isSnsMember(dto.getMemberId())) {
            throw new SecurityException("SNS 회원은 연락처와 주소를 수정할 수 없습니다.");
        }

        String phone = dto.getPhone();

        if (phone == null ||
            !phone.matches(
                "010\\d{8}|010-\\d{4}-\\d{4}|" +
                "01[16789]\\d{7,8}|01[16789]-\\d{3,4}-\\d{4}"
            )) {
            throw new IllegalArgumentException("올바른 휴대폰 번호를 입력해주세요.");
        }

        dto.setPhone(phone.replace("-", ""));

        String address = dto.getAddress();

        if (address != null) {
            address = address.trim();
            
            if (address.length() > 500) {
                throw new IllegalArgumentException("주소는 500자 이내로 입력해주세요.");
            }

            if (address.isEmpty()) {
                address = null;
            }
        }
        
        dto.setAddress(address);

        if (mapper.countActivePhone(dto) > 0) {
            throw new IllegalStateException("이미 사용 중인 연락처입니다.");
        }

        int result = mapper.updateContact(dto);

        if (result != 1) {
            throw new IllegalStateException("회원정보를 수정할 수 없습니다.");
        }

        return result;
    }


    @Override
    @Transactional
    public String updateProfile(Long memberId, MultipartFile file) throws Exception {
        MypageDto current = mapper.findMyInfo(memberId);

        if (current == null) {
            throw new IllegalStateException("회원정보를 찾을 수 없습니다.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("사진을 선택해주세요.");
        }

        if (file.getSize() > 800 * 1024) {
            throw new IllegalArgumentException("이미지는 800KB 이하만 가능합니다.");
        }

        String originalName = file.getOriginalFilename();
        String type = file.getContentType();

        if (originalName == null ||
                !originalName.toLowerCase().matches(".*\\.(jpg|jpeg|png)") ||
                !("image/jpeg".equals(type) || "image/png".equals(type)) ||
                ImageIO.read(file.getInputStream()) == null) {

            throw new IllegalArgumentException("JPG 또는 PNG 이미지만 가능합니다.");
        }

        String filename = null;

        try {
            filename = storageService.uploadFileToServer(file, uploadPath);

            if (filename == null || filename.isBlank()) {
                throw new IllegalStateException("사진 업로드에 실패했습니다.");
            }

            MypageDto dto = new MypageDto();
            dto.setMemberId(memberId);
            dto.setProfile(filename);

            if (mapper.updateProfile(dto) != 1) {
                throw new IllegalStateException("프로필 사진 변경에 실패했습니다.");
            }

        } catch (Exception e) {
            if (filename != null) {
                deleteProfileFile(filename);
            }
            
            log.error("프로필 사진 변경 오류", e);
            throw e;
        }
        
        deleteProfileFile(current.getProfile());

        return filename;
    }


    @Override
    @Transactional
    public void deleteProfile(Long memberId) throws Exception {
        MypageDto current = mapper.findMyInfo(memberId);

        if (current == null) {
            throw new IllegalStateException("회원정보를 찾을 수 없습니다.");
        }

        MypageDto dto = new MypageDto();
        dto.setMemberId(memberId);
        dto.setProfile(null);

        if (mapper.updateProfile(dto) != 1) {
            throw new IllegalStateException("프로필 사진 삭제에 실패했습니다.");
        }

        deleteProfileFile(current.getProfile());
    }
    
	 @Override
	 @Transactional
	 public String updatePwd(MypagePwdDto dto) throws Exception {
	     if (dto == null || dto.getMemberId() == null) {
	         throw new IllegalArgumentException("회원정보가 올바르지 않습니다.");
	     }
	
	     if (isSnsMember(dto.getMemberId())) {
	         throw new SecurityException("SNS 회원은 비밀번호를 변경할 수 없습니다.");
	     }
	
	     String currentPwd = dto.getCurrentPwd();
	     String newPwd = dto.getNewPwd();
	     String confirmPwd = dto.getConfirmPwd();
	
	     if (currentPwd == null || currentPwd.isBlank()) {
	         throw new IllegalArgumentException("현재 비밀번호를 입력해주세요.");
	     }
	
	     if (newPwd == null || confirmPwd == null) {
	         throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
	     }
	
	     String savedPwd = mapper.findPwdByMemberId(dto.getMemberId());
	
	     if (savedPwd == null || savedPwd.isBlank()) {
	         throw new IllegalStateException("회원 비밀번호를 조회할 수 없습니다.");
	     }
	
	     if (! passwordEncoder.matches(currentPwd, savedPwd)) {
	         throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
	     }
	
	     if (! newPwd.equals(confirmPwd)) {
	         throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
	     }
	
	     if (! newPwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*])[a-zA-Z0-9!@#$%^&*]{8,20}$")) {
	         throw new IllegalArgumentException("영문 대소문자, 숫자, 특수문자를 포함해 8~20자로 입력해주세요.");
	     }
	
	     if (passwordEncoder.matches(newPwd, savedPwd)) {
	         throw new IllegalArgumentException("기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
	     }
	
	     String encodedPwd = passwordEncoder.encode(newPwd);
	     dto.setPwd(encodedPwd);
	     
	     int result = mapper.updatePwd(dto);
	
	     if (result != 1) {
	         throw new IllegalStateException("비밀번호 변경에 실패했습니다.");
	     }
	
	     return encodedPwd;
	 }
	 

	@Override
	public List<MypageWorkDto> findMyWorkspaces(Long memberId) throws Exception {
	    if (memberId == null) {
	        throw new IllegalArgumentException("회원정보가 올바르지 않습니다.");
	    }
	    return mapper.findMyWorkspaces(memberId);
	}



}
