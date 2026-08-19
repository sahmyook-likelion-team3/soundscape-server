package com.soundscape.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank(message = "username은 필수입니다.")
    @Size(min = 4, max = 20, message = "username은 4~20자여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username은 영문, 숫자, 언더스코어만 사용할 수 있습니다.")
    private String username;

    @NotBlank(message = "password는 필수입니다.")
    @Size(min = 8, max = 64, message = "password는 8~64자여야 합니다.")
    private String password;

    @NotBlank(message = "nickname은 필수입니다.")
    @Size(max = 50, message = "nickname은 50자를 넘을 수 없습니다.")
    private String nickname;
}
