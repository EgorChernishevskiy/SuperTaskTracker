package org.example.api.controllers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.api.dto.UserDto;
import org.example.api.exeptions.BadRequestException;
import org.example.api.mappers.UserDtoMapper;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.UserRepository;
import org.example.utils.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@Transactional
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDtoMapper userDtoMapper;

    public static final String REGISTER = "/auth/register";
    public static final String LOGIN = "/auth/login";

    @PostMapping(REGISTER)
    public UserDto register(@RequestParam String name, String password) {

        if (userRepository.findByUsername(name).isPresent()) {
            throw new BadRequestException("Username is already taken");
        }

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }

        if (password.trim().isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }

        UserEntity user = new UserEntity();

        user.setUsername(name);

        user.setPassword(passwordEncoder.encode(password));

        UserEntity savedUser = userRepository.save(user);

        return userDtoMapper.createUserDto(savedUser);
    }

    @PostMapping(LOGIN)
    public String login(@RequestParam String name, String password) {

        UserEntity user = userRepository.findByUsername(name)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getUsername());
    }
}
