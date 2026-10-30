package com.tk.app.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.tk.app.domain.dto.MypageChatDto;
import com.tk.app.domain.dto.MypageDto;
import com.tk.app.domain.dto.MypagePwdDto;
import com.tk.app.domain.dto.MypageWorkDto;

@Mapper
public interface MypageMapper {
	
	public MypageDto findMyInfo(Long memberId);
	public int countSnsAccount(Long memberId);
	
    public int countActivePhone(MypageDto dto);
    public int updateContact(MypageDto dto);
    
    public int updateProfile(MypageDto dto);
    
    public String findPwdByMemberId(Long memberId);
    public int updatePwd(MypagePwdDto dto);
    
    public List<MypageWorkDto> findMyWorkspaces(Long memberId);
    
    public int countMyOngoingWork(Long workMemberId);
    
    public MypageWorkDto findWorkLeaveTarget(MypageWorkDto dto);

    public int deleteMySubTaskMembers(MypageWorkDto dto);
    public int deleteMyTaskMembers(MypageWorkDto dto);

    public int leaveMyProjectMembers(MypageWorkDto dto);
    public int leaveMyWorkMember(MypageWorkDto dto);
  
    public List<MypageChatDto> findMyChatRooms(MypageWorkDto dto);
    public Long lockMyChatRoom(Long chatRoomId);
    public Long findNextGroupMaster(MypageChatDto dto);
    public int transferGroupMasterOnLeave(MypageChatDto dto);
    
    public int leaveMyChatRoomMember(MypageChatDto dto);
    public int countActiveMyChatRoomMembers(MypageChatDto dto);
    public int insertWorkLeaveSystemMessage(MypageChatDto dto);
    public String findChatMemberName(Long workMemberId);

    public int deleteMyChatRoomMessages(MypageChatDto dto);
    public int deleteMyChatRoomMembers(MypageChatDto dto);
    public int deleteMyChatRoom(MypageChatDto dto);
	
    public List<Long> findActiveChatRoomMemberIds(MypageChatDto dto);
    
}
