package com.tk.app.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
public class ManageWorkspaceInviteServiceImpl implements ManageWorkspaceInviteService {

    private final ManageInviteMapper mapper;
    private final MailSender mailSender;
    private final GrowthTreeService growthTreeService;

    @Value("${MAIL_USERNAME}")
    private String mailUsername;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendInvitesAndMail(Map<String, Object> map) throws Exception {
        String domainKey = (String) map.get("domainKey");
        Long workspaceId = (Long) map.get("workspaceId");
        Long inviterId = (Long) map.get("inviterId");
        if (workspaceId == null || inviterId == null) throw new SecurityException("초대자 정보가 없습니다.");
        Map<String, Object> permissionMap = new java.util.HashMap<>();
        permissionMap.put("domainKey", domainKey);
        permissionMap.put("workspaceId", workspaceId);
        permissionMap.put("memberId", inviterId);
        if (mapper.countActiveWorkspacePm(permissionMap) != 1) {
            throw new SecurityException("현재 워크스페이스 PM만 팀원을 초대할 수 있습니다.");
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
    
    @Override
    public List<ManageInviteDto> getSearchEligibleMembers(Map<String, Object> map) {
        try {
            return mapper.selectSearchEligibleMembers(map);
        } catch (Exception e) {
            log.info("getSearchEligibleMembers 오류 : ", e);
            throw new IllegalStateException("초대 대상 조회 중 오류가 발생했습니다.", e);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptInvite(Map<String, Object> map) throws Exception {
        String inviteKey = (String) map.get("inviteKey");
        Long memberId = (Long) map.get("memberId");

        ManageInviteDto inviteDto = mapper.selectManageInviteByKey(inviteKey);
        String domainKey = (String) map.get("domainKey");
        Long domainWorkspaceId = mapper.selectWorkspaceIdByDomainKey(domainKey);
        if (inviteDto == null || domainWorkspaceId == null || !domainWorkspaceId.equals(inviteDto.getWorkspaceId())) {
            throw new IllegalArgumentException("현재 도메인에서 유효하지 않은 초대 링크입니다.");
        }
        try {
            java.util.Date expiry = new SimpleDateFormat(DATE_FORMAT).parse(inviteDto.getExpires());
            if (inviteDto.getStatus() == null || inviteDto.getStatus() != 0 || !expiry.after(new java.util.Date())) {
                throw new IllegalArgumentException("기간이 만료된 초대 링크입니다.");
            }
            growthTreeService.grantInviteAcceptExp(inviteDto.getWorkspaceId(), memberId, inviteKey);

        } catch (java.text.ParseException e) {
            throw new IllegalArgumentException("초대 만료일이 올바르지 않습니다.", e);
        }
        if (inviteDto.getInviteStatus() != null && inviteDto.getInviteStatus() == 1) {
            throw new IllegalArgumentException("이미 사용이 완료된 초대 링크입니다.");
        }

        inviteDto.setAcceptedMemberId(memberId);
        inviteDto.setInviteStatus(1);
        if (mapper.updateManageInviteAccept(inviteDto) != 1) {
            throw new IllegalArgumentException("이미 사용되었거나 만료된 초대 링크입니다.");
        }

        inviteDto.setMemberId(memberId);
        inviteDto.setRole(10);
        inviteDto.setStatus(0);
        
        growthTreeService.grantInviteAcceptExp(inviteDto.getWorkspaceId(), memberId, inviteKey);
    }
}