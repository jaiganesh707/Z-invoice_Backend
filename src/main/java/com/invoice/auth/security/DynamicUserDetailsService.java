package com.invoice.auth.security;

import com.invoice.auth.entity.User;
import com.invoice.auth.entity.Employee;
import com.invoice.auth.repository.UserRepository;
import com.invoice.auth.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DynamicUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Try Primary Ledger (users table)
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // 2. Try Combined Employee Hub (employees table)
        Optional<Employee> empOpt = employeeRepository.findByUsername(username);
        if (empOpt.isPresent() && empOpt.get().getPassword() != null) {
            Employee emp = empOpt.get();
            User empUser = User.builder()
                    .id(emp.getId())
                    .username(emp.getUsername())
                    .email(emp.getEmail())
                    .password(emp.getPassword())
                    .role(emp.getRole())
                    .uniqueCode(emp.getUniqueKey())
                    .contactNumber(emp.getContactNumber())
                    .build();

            // Resolve Prime User via uniqueKey prefix (e.g. JACK-1001 -> user with username 'jack')
            if (emp.getUniqueKey() != null && emp.getUniqueKey().contains("-")) {
                String primeUsername = emp.getUniqueKey().split("-")[0].toLowerCase();
                userRepository.findByUsername(primeUsername).ifPresent(empUser::setParentUser);
            }
            return empUser;
        }


        throw new UsernameNotFoundException("Identity node not found for username: " + username);
    }
}
