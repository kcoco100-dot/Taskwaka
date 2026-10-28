package com.tk.app.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tk.app.domain.dto.ManageInviteDto;
import com.tk.app.mail.Mail;
import com.tk.app.mail.MailSender;
import com.tk.app.mapper.ManageInviteMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageWorkspaceInviteServiceImpl implements ManageWorkspaceInviteService {

    private final ManageInviteMapper mapper;
    private final MailSender mailSender;
    private final GrowthTreeService growthTreeService;

    @Value("${MAIL_USERNAME}")
    private String mailUsername;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // 팀원 초대 및 메일 전송
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendInvitesAndMail(Map<String, Object> map) throws Exception {
        String domainKey = (String) map.get("domainKey");
        Long workspaceId = (Long) map.get("workspaceId");
        Long inviterId = (Long) map.get("inviterId");
        if (workspaceId == null || inviterId == null) {
            throw new SecurityException("초대자 정보가 없습니다.");
        }
        
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

            sendMail(domainKey, targetInfo.getEmail(), targetInfo.getName(), inviteKey);
        }
    }

    // 초대 메일 전송
    private void sendMail(String domainKey, String toEmail, String name, String inviteKey) {
        String inviteLink = "http://localhost:9090/main?inviteKey=" + inviteKey;
        
        Mail mail = new Mail();
        mail.setReceiverEmail(toEmail);
        mail.setSenderEmail(mailUsername); 
        mail.setSenderName("Taskwaka 시스템");
        mail.setSubject("[Taskwaka] 워크스페이스 팀원 초대 메일입니다.");
        
        String htmlContent = "안녕하세요, <strong>" + name + "</strong>님!<br><br>" +
                             "Taskwaka 워크스페이스에 참여하도록 초대되었습니다.<br>" +
                             "아래 링크를 통해 로그인하신 후 초대를 수락해 주시기 바랍니다.<br><br>" +
                             "▶ <a href='" + inviteLink + "' style='color:blue;'>초대 수락 링크 접속하기</a>";
        mail.setContent(htmlContent);

        boolean isSuccess = mailSender.mailSend(mail);
        
        if (!isSuccess) {
            log.error("메일 발송 실패: {}", toEmail);
            throw new RuntimeException("이메일 발송에 실패했습니다."); 
        }
    }
    
    // 초대 대상 검색
    @Override
    public List<ManageInviteDto> getSearchEligibleMembers(Map<String, Object> map) {
        List<ManageInviteDto> result = new java.util.ArrayList<>();
        try {
            result = mapper.selectSearchEligibleMembers(map);
        } catch (Exception e) {
            log.info("getSearchEligibleMembers 오류 : ", e);
            throw new IllegalStateException("초대 대상 조회 중 오류가 발생했습니다.", e);
        }
        return result;
    }
    
    // 초대 수락
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvite(Map<String, Object> map) throws Exception {
        try {
            String inviteKey = (String) map.get("inviteKey");
            Long memberId = (Long) map.get("memberId");
            if (inviteKey == null || inviteKey.isBlank() || memberId == null) {
                throw new IllegalArgumentException("초대 링크와 로그인 정보를 확인하세요.");
            }
            ManageInviteDto invite = mapper.selectManageInviteByKey(inviteKey);
            if (invite == null || !Integer.valueOf(0).equals(invite.getStatus())) {
                throw new IllegalArgumentException("유효하지 않거나 취소된 초대 링크입니다.");
            }
            if (mapper.lockActiveInviteMember(memberId) == null) {
                throw new IllegalArgumentException("가입할 수 있는 회원이 아닙니다.");
            }
            ManageInviteDto member = mapper.selectMemberInfoForInvite(memberId);
            if (member == null || member.getEmail() == null || member.getEmail().isBlank()
                    || invite.getEmail() == null || invite.getEmail().isBlank()
                    || !invite.getEmail().trim().equalsIgnoreCase(member.getEmail().trim())) {
                throw new IllegalArgumentException("초대받은 이메일과 로그인한 회원 이메일이 다릅니다. 초대받은 계정으로 로그인하세요.");
            }
            String domainKey = (String) map.get("domainKey");
            if (domainKey == null || domainKey.isBlank()) {
                domainKey = mapper.selectInviteDomainByWorkspaceId(invite.getWorkspaceId());
            }
            if (domainKey == null || !domainKey.matches("[a-zA-Z0-9-]+")
                    || !invite.getWorkspaceId().equals(mapper.selectWorkspaceIdByDomainKey(domainKey))) {
                throw new IllegalArgumentException("초대된 워크의 주소가 올바르지 않습니다.");
            }
            Map<String, Object> memberMap = new java.util.HashMap<>();
            memberMap.put("workspaceId", invite.getWorkspaceId());
            memberMap.put("memberId", memberId);
            ManageInviteDto existing = mapper.selectInviteWorkspaceMember(memberMap);
            boolean active = existing != null && Integer.valueOf(0).equals(existing.getStatus());
            if (Integer.valueOf(1).equals(invite.getInviteStatus())) {
                if (memberId.equals(invite.getAcceptedMemberId()) && active) {
                    map.put("domainKey", domainKey);
                    return;
                }
                throw new IllegalArgumentException("이미 사용된 초대입니다. 워크에서 제외된 경우 새 초대를 받으세요.");
            }
            if (!Integer.valueOf(0).equals(invite.getInviteStatus())) {
                throw new IllegalArgumentException("유효하지 않은 초대 상태입니다.");
            }
            if (existing != null && !active && !Integer.valueOf(1).equals(existing.getStatus())) {
                throw new IllegalArgumentException("현재 워크 참여 상태에서는 초대를 수락할 수 없습니다.");
            }
            invite.setAcceptedMemberId(memberId);
            invite.setInviteStatus(1);
            if (mapper.updateManageInviteAccept(invite) != 1) {
                throw new IllegalArgumentException("만료되었거나 이미 사용된 초대 링크입니다.");
            }
            invite.setMemberId(memberId);
            invite.setRole(10);
            invite.setStatus(0);
            mapper.insertManageInviteMember(invite);
            ManageInviteDto saved = mapper.selectInviteWorkspaceMember(memberMap);
            if (saved == null || !Integer.valueOf(0).equals(saved.getStatus())) {
                throw new IllegalStateException("워크 가입이 완료되지 않았습니다.");
            }
            if (!active) {
                growthTreeService.grantInviteAcceptExp(invite.getWorkspaceId(), memberId, inviteKey);
            }
            map.put("domainKey", domainKey);
        } catch (Exception e) {
            log.info("acceptInvite : ", e);
            throw e;
        }
    }
}
