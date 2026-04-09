package com.invoice.auth.service;

import com.invoice.auth.dto.LoginUserDto;
import com.invoice.auth.dto.RegisterUserDto;
import com.invoice.auth.entity.Employee;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.exception.UserAlreadyExistsException;
import com.invoice.auth.repository.EmployeeRepository;
import com.invoice.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("AuthenticationService Unit Tests")
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("primeuser");
        testUser.setEmail("prime@example.com");
        testUser.setRole(RoleEnum.ROLE_PRIME_USER);
        testUser.setUniqueCode("USR-ABC12345");

        testEmployee = new Employee();
        testEmployee.setId(10);
        testEmployee.setUsername("emp1");
        testEmployee.setEmail("emp1@example.com");
        testEmployee.setRole(RoleEnum.ROLE_WORKFLOW_USER);
        testEmployee.setUniqueKey("EMP-XYZ12345");
        testEmployee.setUser(testUser);
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("should register a new user successfully")
        void testSignup_Success() {
            RegisterUserDto dto = buildRegisterDto("newuser", "new@example.com");
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = authenticationService.signup(dto);

            assertNotNull(result);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw when username is already taken")
        void testSignup_DuplicateUsername() {
            RegisterUserDto dto = buildRegisterDto("primeuser", "other@example.com");
            when(userRepository.findByUsername("primeuser")).thenReturn(Optional.of(testUser));

            assertThrows(UserAlreadyExistsException.class, () -> authenticationService.signup(dto));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when email is already registered")
        void testSignup_DuplicateEmail() {
            RegisterUserDto dto = buildRegisterDto("newuserX", "prime@example.com");
            when(userRepository.findByUsername("newuserX")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("prime@example.com")).thenReturn(Optional.of(testUser));

            assertThrows(UserAlreadyExistsException.class, () -> authenticationService.signup(dto));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("should authenticate a regular user from users table")
        void testAuthenticate_PrimaryUser() {
            LoginUserDto dto = new LoginUserDto();
            dto.setUsername("primeuser");
            dto.setPassword("password123");

            doNothing().when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            when(userRepository.findByUsername("primeuser")).thenReturn(Optional.of(testUser));

            User result = authenticationService.authenticate(dto);

            assertNotNull(result);
            assertEquals("primeuser", result.getUsername());
        }

        @Test
        @DisplayName("should authenticate an employee and link parentUser")
        void testAuthenticate_Employee() {
            LoginUserDto dto = new LoginUserDto();
            dto.setUsername("emp1");
            dto.setPassword("emp_password");

            doNothing().when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            when(userRepository.findByUsername("emp1")).thenReturn(Optional.empty());
            when(employeeRepository.findByUsername("emp1")).thenReturn(Optional.of(testEmployee));

            User result = authenticationService.authenticate(dto);

            assertNotNull(result);
            assertEquals("emp1", result.getUsername());
            assertNotNull(result.getParentUser());
            assertEquals(testUser, result.getParentUser());
        }

        @Test
        @DisplayName("should throw when neither user nor employee found post-auth")
        void testAuthenticate_NotFound() {
            LoginUserDto dto = new LoginUserDto();
            dto.setUsername("ghost");
            dto.setPassword("nopass");

            doNothing().when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
            when(employeeRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> authenticationService.authenticate(dto));
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("should update email and contactNumber for valid user")
        void testUpdateUser_Success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authenticationService.updateUser(1, "updated@example.com", "9999999999");

            assertEquals("updated@example.com", result.getEmail());
            assertEquals("9999999999", result.getContactNumber());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when id is null")
        void testUpdateUser_NullId() {
            assertThrows(IllegalArgumentException.class,
                    () -> authenticationService.updateUser(null, "x@x.com", "123"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for missing user")
        void testUpdateUser_NotFound() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(com.invoice.auth.exception.ResourceNotFoundException.class,
                    () -> authenticationService.updateUser(999, "x@x.com", null));
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("should successfully delete a user by ID")
        void testDelete_Success() {
            when(userRepository.existsById(1)).thenReturn(true);
            doNothing().when(userRepository).deleteById(1);

            assertDoesNotThrow(() -> authenticationService.deleteUser(1));
            verify(userRepository).deleteById(1);
        }

        @Test
        @DisplayName("should throw when user not found for deletion")
        void testDelete_NotFound() {
            when(userRepository.existsById(999)).thenReturn(false);
            assertThrows(com.invoice.auth.exception.ResourceNotFoundException.class,
                    () -> authenticationService.deleteUser(999));
        }
    }

    private RegisterUserDto buildRegisterDto(String username, String email) {
        RegisterUserDto dto = new RegisterUserDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setPassword("password123");
        dto.setRole(RoleEnum.ROLE_PRIME_USER);
        return dto;
    }
}
