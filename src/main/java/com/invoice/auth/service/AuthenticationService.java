package com.invoice.auth.service;

import com.invoice.auth.exception.UserAlreadyExistsException;

import com.invoice.auth.dto.LoginUserDto;
import com.invoice.auth.dto.RegisterUserDto;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.entity.Employee;
import com.invoice.auth.repository.UserRepository;
import com.invoice.auth.repository.EmployeeRepository;
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
    private final EmployeeRepository employeeRepository;
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
                .upiId(input.getUpiId())
                .payeeName(input.getPayeeName())
                .currency(input.getCurrency() != null ? input.getCurrency() : "INR")
                .build();

        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getUsername(),
                        input.getPassword()));

        // 1. Try primary users table
        java.util.Optional<User> userOpt = userRepository.findByUsername(input.getUsername());
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 2. Try employees table (Approvers, Creators, HR stored here)
        java.util.Optional<Employee> empOpt = employeeRepository.findByUsername(input.getUsername());
        if (empOpt.isPresent()) {
            Employee emp = empOpt.get();

            // Build a User proxy from the Employee
            User empUser = User.builder()
                    .id(emp.getId())
                    .username(emp.getUsername())
                    .email(emp.getEmail())
                    .password(emp.getPassword())
                    .role(emp.getRole())
                    .uniqueCode(emp.getUniqueKey())
                    .contactNumber(emp.getContactNumber())
                    .build();

            // Resolve Prime User (Owner) to link the employee session to their business group
            if (emp.getUser() != null) {
                empUser.setParentUser(emp.getUser());
            }

            return empUser;
        }

        throw new RuntimeException("User not found after authentication");
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
                .orElseThrow(() -> new com.invoice.auth.exception.ResourceNotFoundException("User not found"));

        if (email != null)
            user.setEmail(email);
        if (contactNumber != null)
            user.setContactNumber(contactNumber);

        return userRepository.save(user);
    }

    @SuppressWarnings("null")
    public User updateFullUser(Integer id, RegisterUserDto input) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.invoice.auth.exception.ResourceNotFoundException("User not found"));

        user.setUsername(input.getUsername());
        user.setEmail(input.getEmail());
        if (input.getPassword() != null && !input.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(input.getPassword()));
        }
        user.setContactNumber(input.getContactNumber());
        if (input.getRole() != null) {
            user.setRole(input.getRole());
        }
        user.setUpiId(input.getUpiId());
        user.setPayeeName(input.getPayeeName());
        user.setCurrency(input.getCurrency() != null ? input.getCurrency() : "INR");

        return userRepository.save(user);
    }

    @SuppressWarnings("null")
    public User updateImageUrl(Integer id, String imageUrl) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.invoice.auth.exception.ResourceNotFoundException("User not found"));
        user.setImageUrl(imageUrl);
        return userRepository.save(user);
    }

    @SuppressWarnings("null")
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new com.invoice.auth.exception.ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(id);
    }
}
