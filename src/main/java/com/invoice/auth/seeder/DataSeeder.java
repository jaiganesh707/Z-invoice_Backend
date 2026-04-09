package com.invoice.auth.seeder;

import com.invoice.auth.entity.FoodItem;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.entity.User;
import com.invoice.auth.repository.FoodItemRepository;
import com.invoice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationListener<ContextRefreshedEvent> {
    private final UserRepository userRepository;
    private final FoodItemRepository foodItemRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        this.createSuperAdmin();
        this.createPrimeUser();
        this.migrateLegacyRoles();
        this.seedFoodItems();
    }

    private void migrateLegacyRoles() {
        userRepository.findAll().forEach(user -> {
            if (user.getRole() == RoleEnum.ROLE_ADMIN) {
                user.setRole(RoleEnum.ROLE_USER);
                userRepository.save(user);
            }
        });
    }

    @SuppressWarnings("null")
    private void createSuperAdmin() {
        Optional<User> optionalUser = userRepository.findByEmail("super.admin@email.com");

        if (optionalUser.isEmpty()) {
            User user = User.builder()
                    .username("superadmin")
                    .email("super.admin@email.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(RoleEnum.ROLE_SUPER_ADMIN)
                    .uniqueCode("USR-ADMIN-01")
                    .build();

            userRepository.save(user);
        }
    }

    @SuppressWarnings("null")
    private void createPrimeUser() {
        Optional<User> optionalUser = userRepository.findByEmail("prime@galaxy.com");

        if (optionalUser.isEmpty()) {
            User user = User.builder()
                    .username("primeboss")
                    .email("prime@galaxy.com")
                    .password(passwordEncoder.encode("password"))
                    .role(RoleEnum.ROLE_PRIME_USER)
                    .uniqueCode("USR-PRIME-01")
                    .upiId("prime@oksbi")
                    .payeeName("Prime Corporation")
                    .currency("INR")
                    .build();

            userRepository.save(user);
        }
    }

    @SuppressWarnings("null")
    private void seedFoodItems() {
        if (foodItemRepository.count() == 0) {
            User superAdmin = userRepository.findByUsername("superadmin").orElse(null);

            List<FoodItem> foodItems = List.of(
                    FoodItem.builder().name("Dummy").price(new BigDecimal("00.00"))
                            .description("dummy").user(superAdmin)
                            .uniqueCode("ITM-INIT-01").build());
            foodItemRepository.saveAll(foodItems);
        }
    }
}
