package com.tk.app.security;

import java.util.Comparator;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;

public class LoginSessionRegistry extends SessionRegistryImpl {
    private volatile boolean limited = true;

    public boolean isLimited() { return limited; }

    public synchronized void configure(boolean enabled) {
        limited = enabled;
        if (enabled) {
            for (Object principal : getAllPrincipals()) {
                getAllSessions(principal, false).stream()
                    .sorted(Comparator.comparing(SessionInformation::getLastRequest).reversed())
                    .skip(1).forEach(SessionInformation::expireNow);
            }
        }
    }

    @Override
    public synchronized void registerNewSession(String sessionId, Object principal) {
        super.registerNewSession(sessionId, principal);
        if (limited) {
            getAllSessions(principal, false).stream().filter(previous -> !sessionId.equals(previous.getSessionId()))
                    .forEach(SessionInformation::expireNow);
        }
    }
}
