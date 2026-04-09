package com.invoice.auth.service;

import com.invoice.auth.dto.DriverDetailsDto;
import com.invoice.auth.dto.DriverRegisterDto;
import com.invoice.auth.entity.Driver;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.DriverRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Driver registerDriver(DriverRegisterDto dto) {
        User user = User.builder()
                .username(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(RoleEnum.ROLE_DRIVER)
                .contactNumber(dto.getContactNumber())
                .currency("INR")
                .build();

        @SuppressWarnings("null") User savedUser = userRepository.save(user);

        Driver driver = Driver.builder()
                .user(savedUser)
                .age(dto.getAge())
                .address(dto.getAddress())
                .licenseNumber(dto.getLicenseNumber())
                .build();

        @SuppressWarnings("null") Driver savedDriver = driverRepository.save(driver);
        return savedDriver;
    }

    public Driver updateDriverDetails(DriverDetailsDto dto) {
        Integer userId = dto.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.invoice.auth.exception.ResourceNotFoundException("User not found"));

        if (user.getRole() != RoleEnum.ROLE_DRIVER) {
            user.setRole(RoleEnum.ROLE_DRIVER);
            userRepository.save(user);
        }

        Optional<Driver> existingDriver = driverRepository.findByUserId(user.getId());
        Driver driver = existingDriver.orElse(new Driver());
        driver.setUser(user);
        driver.setAge(dto.getAge());
        driver.setBikeNo(dto.getBikeNo());
        driver.setLicenseNumber(dto.getLicenseNumber());
        driver.setLicensePhoto(dto.getLicensePhoto());
        driver.setDriverPhoto(dto.getDriverPhoto());
        driver.setAddress(dto.getAddress());

        return Objects.requireNonNull(driverRepository.save(driver));
    }

    public List<DriverDetailsDto> getAllDrivers() {
        List<DriverDetailsDto> driverList = new ArrayList<>();
        driverRepository.findAll().forEach(driver -> {
            driverList.add(mapToDto(driver));
        });
        return driverList;
    }

    public List<DriverDetailsDto> getDriversByParent(User parentUser) {
        List<DriverDetailsDto> driverList = new ArrayList<>();
        driverRepository.findByUserParentUser(parentUser).forEach(driver -> {
            driverList.add(mapToDto(driver));
        });
        return driverList;
    }

    public Optional<Driver> getDriverByUserId(Integer userId) {
        return driverRepository.findByUserId(userId);
    }

    public DriverDetailsDto getDriverDetails(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        return driverRepository.findByUserId(userId)
                .map(this::mapToDto)
                .orElseThrow(
                        () -> new com.invoice.auth.exception.ResourceNotFoundException("Driver details not found"));
    }

    public Driver updateLicenseUrl(Integer userId, String url) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new com.invoice.auth.exception.ResourceNotFoundException("Driver not found"));
        driver.setLicensePhoto(url);
        return Objects.requireNonNull(driverRepository.save(driver));
    }

    public Driver updateDriverPhotoUrl(Integer userId, String url) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new com.invoice.auth.exception.ResourceNotFoundException("Driver not found"));
        driver.setDriverPhoto(url);
        return Objects.requireNonNull(driverRepository.save(driver));
    }

    private DriverDetailsDto mapToDto(Driver driver) {
        return DriverDetailsDto.builder()
                .userId(driver.getUser().getId())
                .name(driver.getUser().getUsername())
                .email(driver.getUser().getEmail())
                .contactNumber(driver.getUser().getContactNumber())
                .age(driver.getAge())
                .bikeNo(driver.getBikeNo())
                .licenseNumber(driver.getLicenseNumber())
                .licensePhoto(driver.getLicensePhoto())
                .driverPhoto(driver.getDriverPhoto())
                .address(driver.getAddress())
                .build();
    }
}
