package com.invoice.auth.service;

import com.invoice.auth.dto.WorkflowUserDto;
import com.invoice.auth.entity.Employee;
import com.invoice.auth.entity.User;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.repository.EmployeeRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<WorkflowUserDto> getSubUsers(Integer parentId) {
        User parent = userRepository.findById(Objects.requireNonNull(parentId))
                .orElseThrow(() -> new RuntimeException("Governing node not found"));
        
        return employeeRepository.findByUser(parent).stream()
                .filter(e -> e.getRole() == RoleEnum.ROLE_APPROVER || e.getRole() == RoleEnum.ROLE_WORKFLOW_USER || e.getRole() == RoleEnum.ROLE_HR)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowUserDto createSubUser(Integer parentId, WorkflowUserDto dto) {
        User parent = userRepository.findById(Objects.requireNonNull(parentId))
                .orElseThrow(() -> new RuntimeException("Governing node not found"));

        // Check for duplicate username or email in employees table
        if (employeeRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new com.invoice.auth.exception.UserAlreadyExistsException(
                "Username '" + dto.getUsername() + "' already exists in the employee hub. Please choose a different username.");
        }
        if (employeeRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new com.invoice.auth.exception.UserAlreadyExistsException(
                "Email '" + dto.getEmail() + "' is already registered in the employee hub.");
        }

        // Generate Prime-User-Based UniqueKey
        long count = employeeRepository.countByUser(parent);
        String uniqueKey = parent.getUsername().toUpperCase() + "-" + (1000 + count + 1);

        Employee employee = Employee.builder()
                .name(dto.getUsername())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .uniqueKey(uniqueKey)
                .contactNumber(dto.getContactNumber())
                .user(parent)
                .status(Employee.EmployeeStatus.ACTIVE)
                .build();

        // Ensure both the argument and the return value of save() are treated as non-null to satisfy strict null-safety analysis
        return mapToDto(Objects.requireNonNull(employeeRepository.save(Objects.requireNonNull(employee))));
    }


    @Transactional
    public WorkflowUserDto updateSubUser(Integer subUserId, WorkflowUserDto dto) {
        Employee employee = employeeRepository.findById(Objects.requireNonNull(subUserId))
                .orElseThrow(() -> new RuntimeException("Operational unit not found"));
        
        employee.setUsername(dto.getUsername());
        employee.setEmail(dto.getEmail());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        employee.setRole(dto.getRole());
        employee.setContactNumber(dto.getContactNumber());
        
        // Ensure both the argument and return of save() fulfill non-null requirements
        return mapToDto(Objects.requireNonNull(employeeRepository.save(Objects.requireNonNull(employee))));
    }

    @Transactional
    public void deleteSubUser(Integer subUserId, Integer parentId) {
        employeeRepository.deleteById(Objects.requireNonNull(subUserId));
    }

    private WorkflowUserDto mapToDto(Employee employee) {
        return WorkflowUserDto.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .email(employee.getEmail())
                .role(employee.getRole())
                .contactNumber(employee.getContactNumber())
                .parentUserId(employee.getUser() != null ? employee.getUser().getId() : null)
                .uniqueKey(employee.getUniqueKey())
                .build();
    }
}
