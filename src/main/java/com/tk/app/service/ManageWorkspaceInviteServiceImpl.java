package com.tk.app.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Value("${taskwaka.public-base-url:}")
    private String publicBaseUrl;
    private final PlatformTransactionManager transactions;

    @Override
    public List<Long> sendInvitesAndMail(Map<String, Object> map) {
        URI base;
        try {
            base = URI.create(publicBaseUrl);
            if (!Set.of("https", "http").contains(base.getScheme()) || base.getHost() == null ||
                    base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null)
                throw new IllegalArgumentException();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "팀 서버의 공개 주소(taskwaka.public-base-url)를 설정해 주세요.");
        }
        var ids = (List<?>) map.get("targetMemberIds");
        var targets = new LinkedHashSet<Long>();
        for (Object value : ids) {
            if (!(value instanceof Number n) || n.longValue() <= 0)
                throw new IllegalArgumentException("초대 대상을 확인하세요.");
            targets.add(n.longValue());
        }
        var failures = new ArrayList<Long>();
        var retry = new HashSet<Long>();
        if (map.get("retryIds") instanceof List<?> previous) {
            for (Object id : previous) if (id instanceof Number n) retry.add(n.longValue());
        }
        boolean resend = Boolean.TRUE.equals(map.get("resend"));
        var tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        for (long id : targets) {
            try {
                Dispatch dispatch = tx.execute(status -> {
                    if (mapper.lockActiveInviteMember(id) == null) throw new IllegalArgumentException("대상 계정이 없습니다.");
                    var target = mapper.selectMemberInfoForInvite(id);
                    if (target == null || target.getEmail() == null || target.getEmail().isBlank())
                        throw new IllegalArgumentException("초대할 이메일이 없습니다.");
                    target.setWorkspaceId((Long) map.get("workspaceId"));
                    target.setInviterId((Long) map.get("inviterId"));
                    String pending = mapper.pendingKey(target);
                    if (pending != null) {
                        target.setInviteKey(pending);
                        return new Dispatch(target, resend || retry.contains(id));
                    }
                    target.setInviteKey(UUID.randomUUID().toString());
                    target.setExpires(LocalDateTime.now().plusDays(7)
                            .format(DateTimeFormatter.ofPattern(DATE_FORMAT)));
                    target.setStatus(0);
                    target.setInviteStatus(0);
                    mapper.insertManageInvite(target);
                    return new Dispatch(target, true);
                });
                if (dispatch.send()) {
                    var invite = dispatch.invite();
                    sendMail(invite.getEmail(), invite.getName(), invite.getInviteKey());
                }
            } catch (RuntimeException e) {
                failures.add(id);
            }
        }
        return failures;
    }

    private record Dispatch(ManageInviteDto invite, boolean send) {}

    private void sendMail(String toEmail, String name, String inviteKey) {
        String inviteLink = UriComponentsBuilder.fromUriString(publicBaseUrl)
                .pathSegment("member", "invite").queryParam("inviteKey", inviteKey).build().toUriString();
        name = HtmlUtils.htmlEscape(name == null ? "" : name);
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
        List<ManageInviteDto> result = new ArrayList<>();
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
            Map<String, Object> memberMap = new HashMap<>();
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
