package org.example.api;

import org.example.api.controllers.AuthController;
import org.example.api.dto.UserDto;
import org.example.api.exceptions.BadRequestException;
import org.example.api.mappers.UserDtoMapper;
import org.example.store.entities.UserEntity;
import org.example.store.repositories.UserRepository;
import org.example.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDtoMapper userDtoMapper;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void register_successful() {
        // Arrange
        String name = "testUser";
        String password = "password123";

        when(userRepository.findByUsername(name)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(name);
        userEntity.setPassword("encodedPassword");

        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        UserDto userDto = new UserDto();
        userDto.setUsername(name);
        when(userDtoMapper.createUserDto(any(UserEntity.class))).thenReturn(userDto);

        // Act
        UserDto result = authController.register(name, password);

        // Assert
        assertNotNull(result);
        assertEquals(name, result.getUsername());
        verify(userRepository).findByUsername(name);
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_nameAlreadyTaken_throwsException() {
        // Arrange
        String name = "testUser";
        String password = "password123";
        when(userRepository.findByUsername(name)).thenReturn(Optional.of(new UserEntity()));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authController.register(name, password));
        assertEquals("Username is already taken", exception.getMessage());
    }

    @Test
    void login_successful() {
        // Arrange
        String name = "testUser";
        String password = "password123";

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(name);
        userEntity.setPassword("encodedPassword");

        when(userRepository.findByUsername(name)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(name)).thenReturn("mockedJwtToken");

        // Act
        String result = authController.login(name, password);

        // Assert
        assertNotNull(result);
        assertEquals("mockedJwtToken", result);
        verify(userRepository).findByUsername(name);
        verify(passwordEncoder).matches(password, "encodedPassword");
        verify(jwtUtil).generateToken(name);
    }

    @Test
    void login_invalidPassword_throwsException() {
        // Arrange
        String name = "testUser";
        String password = "wrongPassword";

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(name);
        userEntity.setPassword("encodedPassword");

        when(userRepository.findByUsername(name)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.matches(password, "encodedPassword")).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authController.login(name, password));
        assertEquals("Invalid credentials", exception.getMessage());
        verify(passwordEncoder).matches(password, "encodedPassword");
    }

    @Test
    void login_invalidUsername_throwsException() {

        String name = "wrongUsername";
        String password = "password123";

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(name);
        userEntity.setPassword("encodedPassword");

        when(userRepository.findByUsername(name)).thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authController.login(name, password));
        assertEquals("Invalid credentials", exception.getMessage());
    }
}