package com.tk.app.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class LoginSessionPolicy {
    private final Path file;
    private final LoginSessionRegistry sessions;

    public LoginSessionPolicy(@Value("${taskwaka.session-policy-file:${user.home}/.taskwaka/session-policy}") String file,
                              LoginSessionRegistry sessions) throws IOException {
        this.file = Path.of(file).toAbsolutePath();
        this.sessions = sessions;
        sessions.configure(!Files.exists(this.file) || !"false".equals(Files.readString(this.file).trim()));
    }

    public boolean isLimited() { return sessions.isLimited(); }

    public Integer maximum(Authentication authentication) { return isLimited() ? 1 : -1; }

    public synchronized void change(boolean value) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "session-policy-", ".tmp");
        try {
            Files.writeString(temp, Boolean.toString(value));
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
        sessions.configure(value);
    }
}
