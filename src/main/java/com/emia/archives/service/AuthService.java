package com.emia.archives.service;

import com.emia.archives.dto.UserDto;
import com.emia.archives.dto.request.LoginRequest;
import com.emia.archives.dto.response.LoginResponse;
import com.emia.archives.entity.User;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EntityMapper mapper;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "username", request.getUsername()));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("Identifiants incorrects");
        }

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("Compte désactivé");
        }

        UserDto userDto = mapper.toDto(user);

        return LoginResponse.builder()
                .user(userDto)
                .token("mock-token-" + System.currentTimeMillis())
                .build();
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "username", username));
        return mapper.toDto(user);
    }
}
