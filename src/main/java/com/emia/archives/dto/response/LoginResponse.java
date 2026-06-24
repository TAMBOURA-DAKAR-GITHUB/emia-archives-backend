package com.emia.archives.dto.response;

import com.emia.archives.dto.UserDto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class LoginResponse {

    private UserDto user;
    private String token;
}
