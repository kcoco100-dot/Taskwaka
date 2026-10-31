package com.tk.app.service;

import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.tk.app.chat.WorkChatMessageSavedEvent;
import com.tk.app.common.StorageService;
import com.tk.app.domain.dto.MypageChatDto;
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
	private final ApplicationEventPublisher eventPublisher;

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
    @Transactional(rollbackFor = Exception.class)
    public String updateProfile(Long memberId, MultipartFile file) throws Exception {
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

        MypageDto current = findProfileForUpdate(memberId);
        String filename = storageService.uploadFileToServer(file, uploadPath);
        if (filename == null || filename.isBlank()) {
            throw new IllegalStateException("사진 업로드에 실패했습니다.");
        }
        changeProfile(current, filename);
        return filename;
    }


    @Override
    @Transactional
    public void deleteProfile(Long memberId) throws Exception {
        changeProfile(findProfileForUpdate(memberId), null);
    }

    private MypageDto findProfileForUpdate(Long memberId) {
        // 같은 회원의 사진 교체·삭제·탈퇴가 직전 파일명을 동시에 읽지 않도록 한다.
        if (mapper.lockMemberForWithdrawal(memberId) == null) {
            throw new IllegalStateException("회원정보를 찾을 수 없습니다.");
        }
        MypageDto current = mapper.findMyInfo(memberId);
        if (current == null) {
            throw new IllegalStateException("회원정보를 찾을 수 없습니다.");
        }
        return current;
    }

    private void changeProfile(MypageDto current, String filename) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteProfileFile(current.getProfile());
                } else if (status == STATUS_ROLLED_BACK) {
                    deleteProfileFile(filename);
                } else {
                    log.warn("프로필 변경 트랜잭션 결과 확인 필요: memberId={}, 이전={}, 신규={}",
                            current.getMemberId(), current.getProfile(), filename);
                }
            }
        });
        MypageDto dto = new MypageDto();
        dto.setMemberId(current.getMemberId());
        dto.setProfile(filename);
        if (mapper.updateProfile(dto) != 1) {
            throw new IllegalStateException("프로필 사진 변경에 실패했습니다.");
        }
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
	

	@Override
	public boolean hasOngoingWork(Long memberId, Long workId) throws Exception {
	    if (memberId == null || workId == null || workId <= 0) {
	        throw new IllegalArgumentException("워크 정보가 올바르지 않습니다.");
	    }
	
	    MypageWorkDto target = mapper.findMyWorkspaces(memberId).stream()
	            .filter(work -> workId.equals(work.getWorkId()))
	            .findFirst()
	            .orElse(null);
	
	    if (target == null || target.getRole() == null || target.getRole() == 70) {
	        throw new SecurityException("해당 워크에서 탈퇴할 권한이 없습니다.");
	    }
	    
	    boolean result = mapper.countMyOngoingWork(target.getWorkMemberId()) > 0;
	
	    return result;
	}
	

	@Override
	@Transactional
	public void leaveWork(Long memberId, Long workId) {

		if (memberId == null || workId == null || workId <= 0) {
		    throw new IllegalArgumentException("잘못된 워크 탈퇴 요청입니다.");
		}
	
	    MypageWorkDto work = new MypageWorkDto();
	    work.setMemberId(memberId);
	    work.setWorkId(workId);
	
	    MypageWorkDto target = mapper.findWorkLeaveTarget(work);
	
	    if (target == null) {
	        throw new SecurityException("해당 워크에서 탈퇴할 권한이 없습니다.");
	    }
	
	    work.setWorkMemberId(target.getWorkMemberId());
	
	    MypageDto member = mapper.findMyInfo(memberId);
	
	    String memberName = member != null ? member.getName() : null;
	
	    if (memberName == null || memberName.isBlank()) {
	        memberName = "UNKNOWN";
	    }

	    List<MypageChatDto> chatRooms = mapper.findMyChatRooms(work);
	
	    for (MypageChatDto chat : chatRooms) {
	    	
	        chat.setWorkId(workId);
	        chat.setWorkMemberId(work.getWorkMemberId());
	
	        if (mapper.lockMyChatRoom(chat.getChatRoomId()) == null) {
	            continue;
	        }
	        
	        MypageChatDto currentChat = mapper.findMyChatRoomForLeave(chat);

	        if (currentChat == null) {
	            continue;
	        }

	        chat = currentChat;
	        chat.setWorkId(workId);
	        chat.setWorkMemberId(work.getWorkMemberId());
	
	        boolean groupMaster =
	                "GROUP".equals(chat.getChat())
	                && work.getWorkMemberId().equals(chat.getChatOwnerMemberId());
	
	        boolean activeMember = Integer.valueOf(0).equals(chat.getChatRoomMemberStatus());
	
	        Long nextMaster = null;
	
			if (groupMaster) {
			    nextMaster = mapper.findNextGroupMaster(chat);
			
			    if (nextMaster != null) {
			        chat.setChatTargetMemberId(nextMaster);
			
			        if (mapper.transferGroupMasterOnLeave(chat) != 1) {
			            throw new IllegalStateException("그룹 마스터 위임에 실패했습니다.");
			        }
			
			        if (activeMember) {
			            String nextMasterName = mapper.findChatMemberName(nextMaster);
			
			            if (nextMasterName == null || nextMasterName.isBlank()) {
			                nextMasterName = "회원";
			            }
			
			            chat.setSystemMessage(
			                memberName + "님이 " + nextMasterName + "님에게 마스터 권한을 위임했습니다."
			            );
			
			            if (mapper.insertWorkLeaveSystemMessage(chat) != 1) {
			                throw new IllegalStateException("마스터 위임 메시지 저장에 실패했습니다.");
			            }
			        }
			    }
			}

	        int leaveCount = mapper.leaveMyChatRoomMember(chat);

	        if (activeMember && leaveCount != 1) {
	            throw new IllegalStateException("채팅방 퇴장에 실패했습니다.");
	        }
	
	        int remaining = mapper.countActiveMyChatRoomMembers(chat);
	
	        if (groupMaster && nextMaster == null && remaining > 0) {
	            throw new IllegalStateException("그룹 마스터를 지정할 수 없습니다.");
	        }
	
	        if (remaining == 0) {
	            mapper.deleteMyChatRoomMessages(chat);
	            mapper.deleteMyChatRoomMembers(chat);
	
	            if (mapper.deleteMyChatRoom(chat) != 1) {
	                throw new IllegalStateException("채팅방 삭제에 실패했습니다.");
	            }


			} else {
			
			    if (activeMember) {
			        chat.setSystemMessage(memberName + "님이 채팅방을 나갔습니다.");
			
			        if (mapper.insertWorkLeaveSystemMessage(chat) != 1) {
			            throw new IllegalStateException("퇴장 메시지 저장에 실패했습니다.");
			        }
			    }
			
			    if (activeMember || (groupMaster && nextMaster != null)) {
			
			        List<Long> activeMemberIds = mapper.findActiveChatRoomMemberIds(chat);
			
			        if (!activeMemberIds.isEmpty()) {
			            eventPublisher.publishEvent(new WorkChatMessageSavedEvent(
			                    chat.getChatRoomId(),
			                    memberId,
			                    activeMemberIds
			                )
			            );
			        }
			    }
			}
	    }

	        
	    mapper.deleteMySubTaskMembers(work);
	    mapper.deleteMyTaskMembers(work);
	
	    mapper.leaveMyProjectMembers(work);
	
	    if (mapper.leaveMyWorkMember(work) != 1) {
	        throw new IllegalStateException("워크 탈퇴 처리에 실패했습니다.");
	    }
	}

    @Override
    public boolean hasOtherWorkMembers(Long memberId, Long workId) {
        if (memberId == null || workId == null || workId <= 0) {
            throw new IllegalArgumentException("워크 정보가 올바르지 않습니다.");
        }

        MypageWorkDto work = new MypageWorkDto();
        work.setMemberId(memberId);
        work.setWorkId(workId);

        if (mapper.findOwnedWorkForDelete(work) == null) {
            throw new SecurityException("워크 삭제 권한이 없습니다.");
        }

        return mapper.countOtherActiveWorkMembers(workId) > 0;
    }

    @Override
    @Transactional
    public void deleteOwnedWork(Long memberId, Long workId) {
        if (memberId == null || workId == null || workId <= 0) {
            throw new IllegalArgumentException("워크 정보가 올바르지 않습니다.");
        }

        if (mapper.lockWorkForDelete(workId) == null) {
            throw new IllegalStateException("이미 삭제되었거나 비활성화된 워크입니다.");
        }

        MypageWorkDto work = new MypageWorkDto();
        work.setMemberId(memberId);
        work.setWorkId(workId);

        if (mapper.findOwnedWorkForDelete(work) == null) {
            throw new SecurityException("워크 삭제 권한이 없습니다.");
        }

        if (mapper.countOtherActiveWorkMembers(workId) > 0) {
            throw new IllegalStateException("다른 워크 회원이 있습니다. 사장 교체 신청 후 관리자 승인을 받아주세요.");
        }

        if (mapper.disableMyWorkspace(work) != 1) {
            throw new IllegalStateException("워크 상태 변경에 실패했습니다.");
        }

        if (mapper.deleteMyWorkspaceDomain(work) < 1) {
            throw new IllegalStateException("워크 도메인 비활성화에 실패했습니다.");
        }
    }

    @Override
    public boolean hasActiveWorkspaces(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원정보가 올바르지 않습니다.");
        }

        return mapper.countActiveMemberWorkspaces(memberId) > 0;
    }

    @Override
    @Transactional
    public void withdrawSite(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("회원정보가 올바르지 않습니다.");
        }

        if (mapper.lockMemberForWithdrawal(memberId) == null) {
            throw new SecurityException("회원 탈퇴 권한이 없습니다.");
        }

        if (mapper.countActiveMemberWorkspaces(memberId) > 0) {
            throw new IllegalStateException("활성 워크가 있습니다. 모든 워크에서 탈퇴하거나 소유 워크를 정리해주세요.");
        }

        MypageDto member = mapper.findMyInfo(memberId);
        String filename = member != null ? member.getProfile() : null;

        if (mapper.deleteMyMemberDetails(memberId) != 1) {
            throw new IllegalStateException("회원 상세정보 삭제에 실패했습니다.");
        }

        if (mapper.withdrawMyMember(memberId) != 1) {
            throw new IllegalStateException("회원 탈퇴 상태 변경에 실패했습니다.");
        }

        if (filename != null && ! filename.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteProfileFile(filename);
                }
            });
        }
    }

}


