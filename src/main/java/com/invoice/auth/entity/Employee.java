package com.invoice.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "employees", indexes = {
    @Index(name = "idx_emp_user_id", columnList = "user_id"),
    @Index(name = "idx_emp_email", columnList = "email"),
    @Index(name = "idx_emp_username", columnList = "username"),
    @Index(name = "idx_emp_unique_key", columnList = "uniqueKey"),
    @Index(name = "idx_emp_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;


    private String designation;
    private String department;
    private Double salary;
    private LocalDate joinedDate;
    private String username;


    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    private String imageUrl;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = true)
    private String password;



    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    @Column(unique = true, length = 50)
    private String uniqueKey;


    @Column(length = 20)
    private String contactNumber;


    @com.fasterxml.jackson.annotation.JsonIgnore
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parentUser", "invoices", "expenses"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Owner (Prime User)


    public enum EmployeeStatus {
        ACTIVE, ON_LEAVE, TERMINATED
    }
}
