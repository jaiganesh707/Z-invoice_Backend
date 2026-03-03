package com.invoice.auth.service;

import com.invoice.auth.exception.UserAlreadyExistsException;

import com.invoice.auth.dto.LoginUserDto;
import com.invoice.auth.dto.RegisterUserDto;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @SuppressWarnings("null")
    public User signup(RegisterUserDto input) {
        if (userRepository.findByUsername(input.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username '" + input.getUsername() + "' is already taken");
        }
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email '" + input.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(input.getUsername())
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .contactNumber(input.getContactNumber())
                .role(input.getRole() != null ? input.getRole() : RoleEnum.ROLE_USER)
                .build();

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getUsername(),
                        input.getPassword()));

        return userRepository.findByUsername(input.getUsername())
                .orElseThrow();
    }

    public List<User> allUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    public java.util.Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @SuppressWarnings("null")
    public java.util.Optional<User> findById(Integer id) {
        return userRepository.findById(id);
    }

    @SuppressWarnings("null")
    public User updateUser(Integer id, String email, String contactNumber) {
        if (id == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (email != null)
            user.setEmail(email);
        if (contactNumber != null)
            user.setContactNumber(contactNumber);

        return userRepository.save(user);
    }

    @SuppressWarnings("null")
    public User updateFullUser(Integer id, RegisterUserDto input) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        if (input.getPassword() != null && !input.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(input.getPassword()));
        }
        user.setContactNumber(input.getContactNumber());
        if (input.getRole() != null) {
            user.setRole(input.getRole());
        }

        return userRepository.save(user);
    }

    @SuppressWarnings("null")
    public User updateImageUrl(Integer id, String imageUrl) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setImageUrl(imageUrl);
        return userRepository.save(user);
    }

    @SuppressWarnings("null")
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
}
