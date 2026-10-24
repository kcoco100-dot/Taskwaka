package com.tk.app.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageInviteDto;
import com.tk.app.mapper.ManageInviteMapper;

import com.tk.app.mail.Mail;
import com.tk.app.mail.MailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageInviteServiceImpl implements ManageInviteService {

    private final ManageInviteMapper mapper;
    private final MailSender mailSender; 
    
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // 워크스페이스 번호 조회
    @Override
    public Long getWorkspaceIdByDomainKey(Map<String, Object> map) {
        Long workspaceId = null;
        try {
            String domainKey = (String) map.get("domainKey");
            workspaceId = mapper.selectWorkspaceIdByDomainKey(domainKey);
        } catch (Exception e) {
            log.info("getWorkspaceIdByDomainKey : ", e);
        }
        return workspaceId;
    }

    // 초대 대상 조회
    @Override
    public List<ManageInviteDto> getEligibleMembers(Map<String, Object> map) {
        List<ManageInviteDto> list = null;
        try {
            list = mapper.selectEligibleMembers(map);
        } catch (Exception e) {
            log.info("getEligibleMembers : ", e);
        }
        return list;
    }

    // 팀원 초대 등록
    @Override
    @Transactional(rollbackFor = Exception.class) 
    public void sendInvites(Map<String, Object> map) throws Exception {
        try {
            String domainKey = (String) map.get("domainKey");
            Long workspaceId = (Long) map.get("workspaceId");
            Long inviterId = (Long) map.get("inviterId");
            
            @SuppressWarnings("unchecked")
            List<Integer> targetMemberIds = (List<Integer>) map.get("targetMemberIds");
            
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

            for (Integer rawMemberId : targetMemberIds) {
                Long memberId = Long.valueOf(rawMemberId);

                ManageInviteDto targetInfo = mapper.selectMemberInfoForInvite(memberId);
                if (targetInfo == null || targetInfo.getEmail() == null) {
                    continue; 
                }

                String inviteKey = UUID.randomUUID().toString();

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, 7);
                String expiresStr = sdf.format(cal.getTime());

                ManageInviteDto inviteDto = new ManageInviteDto();
                inviteDto.setInviteKey(inviteKey);
                inviteDto.setWorkspaceId(workspaceId);
                inviteDto.setInviterId(inviterId);
                inviteDto.setEmail(targetInfo.getEmail());
                inviteDto.setName(targetInfo.getName());
                inviteDto.setStatus(0); 
                inviteDto.setExpires(expiresStr); 
                inviteDto.setInviteStatus(0); 

                mapper.insertManageInvite(inviteDto);

                sendInviteEmail(domainKey, targetInfo.getEmail(), targetInfo.getName(), inviteKey);
            }
        } catch (Exception e) {
            log.info("sendInvites : ", e);
            throw e;
        }
    }

    // 초대 메일 전송
    private void sendInviteEmail(String domainKey, String toEmail, String name, String inviteKey) {
        String inviteLink = "http://localhost:8080/" + domainKey + "/manage/invite/accept?inviteKey=" + inviteKey;
        
        Mail mail = new Mail();
        mail.setReceiverEmail(toEmail);
        mail.setSenderName("Taskwaka 시스템"); 
        mail.setSubject("[Taskwaka] 워크스페이스 팀원 초대 안내 메일입니다.");
        
        String htmlContent = "안녕하세요, <strong>" + name + "</strong>님!<br><br>" +
                             "Taskwaka 워크스페이스에 참여하도록 초대되었습니다.<br>" +
                             "아래 링크를 통해 로그인하신 후 초대를 수락해 주시기 바랍니다.<br><br>" +
                             "▶ <a href='" + inviteLink + "' style='color:blue; text-decoration:underline; font-weight:bold;'>초대 수락 링크 접속하기</a><br><br>";
        
        mail.setContent(htmlContent);
        boolean isSuccess = mailSender.mailSend(mail);
        
        if (!isSuccess) {
            log.info("이메일 전송 실패 대상: {}", toEmail);
            throw new RuntimeException("이메일 발송에 실패하여 작업을 취소합니다.");
        }
    }

    // 초대 수락
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvite(Map<String, Object> map) throws Exception {
        try {
            String inviteKey = (String) map.get("inviteKey");
            Long memberId = (Long) map.get("memberId");

            ManageInviteDto inviteDto = mapper.selectManageInviteByKey(inviteKey);

            String domainKey = (String) map.get("domainKey");
            Long domainWorkspaceId = mapper.selectWorkspaceIdByDomainKey(domainKey);
            if (inviteDto == null || domainWorkspaceId == null || !domainWorkspaceId.equals(inviteDto.getWorkspaceId())) {
                throw new IllegalArgumentException("현재 도메인에서 유효하지 않은 초대 링크입니다.");
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            Date expireDate = sdf.parse(inviteDto.getExpires());
            
            if (inviteDto.getStatus() == 1 || expireDate.before(new Date())) {
                throw new IllegalArgumentException("기간이 만료된 초대 링크입니다.");
            }
            
            if (inviteDto.getInviteStatus() != 0) {
                throw new IllegalArgumentException("이미 수락 처리가 완료된 링크입니다.");
            }

            inviteDto.setAcceptedMemberId(memberId);
            inviteDto.setInviteStatus(1);
            if (mapper.updateManageInviteAccept(inviteDto) != 1) {
                throw new IllegalArgumentException("이미 수락되었거나 만료된 초대 링크입니다.");
            }

            inviteDto.setMemberId(memberId);
            inviteDto.setRole(21);
            inviteDto.setStatus(0);

            mapper.insertManageInviteMember(inviteDto);

        } catch (Exception e) {
            log.info("acceptInvite 오류 : ", e);
            throw e;
        }
    }

}
