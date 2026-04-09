package com.invoice.auth.service;

import com.invoice.auth.entity.Employee;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    public List<Employee> getEmployeesByUser(User user) {
        return repository.findByUser(user);
    }

    @Transactional
    @SuppressWarnings("null")
    public Employee createEmployee(Employee employee, User user) {
        String dept = employee.getDepartment().toUpperCase();
        boolean shouldCreateLogin = dept.endsWith("-LEAD");
        com.invoice.auth.entity.RoleEnum loginRole = com.invoice.auth.entity.RoleEnum.ROLE_USER;

        if (dept.contains("DRIVER")) {
            loginRole = shouldCreateLogin ? com.invoice.auth.entity.RoleEnum.ROLE_DRIVER_LEAD : com.invoice.auth.entity.RoleEnum.ROLE_DRIVER;
        } else if (dept.contains("MARKETING")) {
            loginRole = shouldCreateLogin ? com.invoice.auth.entity.RoleEnum.ROLE_MARKETING_LEAD : com.invoice.auth.entity.RoleEnum.ROLE_MARKETING;
        } else if (dept.contains("SUPERVISOR")) {
            loginRole = shouldCreateLogin ? com.invoice.auth.entity.RoleEnum.ROLE_SUPERVISOR_LEAD : com.invoice.auth.entity.RoleEnum.ROLE_SUPERVISOR;
        } else if (dept.contains("OPERATIONS")) {
            loginRole = com.invoice.auth.entity.RoleEnum.ROLE_OPERATIONS;
        } else if (dept.contains("MAINTENANCE")) {
            loginRole = com.invoice.auth.entity.RoleEnum.ROLE_MAINTENANCE;
        }

        if (shouldCreateLogin) {
            String username = (employee.getUsername() != null && !employee.getUsername().isEmpty()) 
                               ? employee.getUsername() : employee.getEmail();
            String password = (employee.getPassword() != null && !employee.getPassword().isEmpty()) 
                               ? employee.getPassword() : "Welcome@123";

            employee.setUsername(username);
            employee.setPassword(passwordEncoder.encode(password));
            employee.setRole(loginRole);
            employee.setUniqueKey("EMP-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }


        employee.setUser(user);

        Employee savedEmployee = repository.save(employee);

        return savedEmployee;
    }

    public Employee updateEmployee(Integer id, Employee updatedData, User user) {
        Objects.requireNonNull(id, "Employee ID must not be null");
        Employee existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        if (!existing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: Not your employee");
        }

        existing.setName(updatedData.getName());
        existing.setEmail(updatedData.getEmail());
        existing.setDesignation(updatedData.getDesignation());
        existing.setDepartment(updatedData.getDepartment());
        existing.setSalary(updatedData.getSalary());
        existing.setStatus(updatedData.getStatus());
        existing.setJoinedDate(updatedData.getJoinedDate());
        
        if (updatedData.getImageUrl() != null) {
            existing.setImageUrl(updatedData.getImageUrl());
        }

        return repository.save(existing);
    }

    @Transactional
    public void deleteEmployee(Integer id, User user) {
        Objects.requireNonNull(id, "Employee ID must not be null");
        Employee existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        if (!existing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Employee login info is now inside the employees table itself, so no need to delete from userRepository

        
        repository.delete(existing);
    }

    public Optional<Employee> findById(Integer id) {
        Objects.requireNonNull(id, "Employee ID must not be null");
        return repository.findById(id);
    }
}
