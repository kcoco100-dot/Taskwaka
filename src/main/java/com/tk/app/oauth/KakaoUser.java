
package com.tk.app.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoUser {
    private Long id;
    private String nickname;
    private String email;
    private boolean email_verified;
    private String name;
    private String birthyear;
    private String birthday;
    private String phoneNumber;
}