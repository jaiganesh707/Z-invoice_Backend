package com.invoice.auth.repository;

import com.invoice.auth.entity.Employee;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    List<Employee> findByUser(User user);
    Optional<Employee> findByEmail(String email);
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Optional<Employee> findByUsername(String username);
    Optional<Employee> findByUniqueKey(String uniqueKey);
    long countByUser(User user);
}

