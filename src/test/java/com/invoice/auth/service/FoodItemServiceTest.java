package com.invoice.auth.service;

import com.invoice.auth.entity.FoodItem;
import com.invoice.auth.entity.User;
import com.invoice.auth.entity.RoleEnum;
import com.invoice.auth.exception.ResourceNotFoundException;
import com.invoice.auth.repository.FoodItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("FoodItemService Unit Tests")
class FoodItemServiceTest {

    @Mock private FoodItemRepository foodItemRepository;

    @InjectMocks
    private FoodItemService foodItemService;

    private User owner;
    private User anotherUser;
    private User superAdmin;
    private FoodItem existingItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        owner = new User();
        owner.setId(1);
        owner.setRole(RoleEnum.ROLE_PRIME_USER);

        anotherUser = new User();
        anotherUser.setId(2);
        anotherUser.setRole(RoleEnum.ROLE_PRIME_USER);

        superAdmin = new User();
        superAdmin.setId(99);
        superAdmin.setRole(RoleEnum.ROLE_SUPER_ADMIN);

        existingItem = FoodItem.builder()
                .id(10)
                .name("Samosa")
                .price(new BigDecimal("15.00"))
                .uniqueCode("ITM-ABCD1234")
                .isActive(true)
                .availableStocks(100)
                .user(owner)
                .build();
    }

    // ─────────────────────── GET ALL ───────────────────────

    @Nested
    @DisplayName("allFoodItems")
    class AllFoodItems {

        @Test
        @DisplayName("should return all active food items globally")
        void testGetAll_Success() {
            when(foodItemRepository.findByIsActiveTrue()).thenReturn(List.of(existingItem));
            List<FoodItem> result = foodItemService.allFoodItems();
            assertEquals(1, result.size());
        }
    }

    // ─────────────────────── GET BY USER ───────────────────────

    @Nested
    @DisplayName("getFoodItemsByUser")
    class GetFoodItemsByUser {

        @Test
        @DisplayName("should return items scoped to a specific user")
        void testGetByUser() {
            when(foodItemRepository.findByUserAndIsActiveTrue(owner)).thenReturn(List.of(existingItem));
            List<FoodItem> result = foodItemService.getFoodItemsByUser(owner);
            assertEquals(1, result.size());
            assertEquals("Samosa", result.get(0).getName());
        }
    }

    // ─────────────────────── CREATE ───────────────────────

    @Nested
    @DisplayName("createFoodItem")
    class CreateFoodItem {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should create a new food item successfully")
        void testCreate_Success() {
            FoodItem input = FoodItem.builder()
                    .name("Paneer Roll")
                    .price(new BigDecimal("50.00"))
                    .build();

            when(foodItemRepository.findAllByUser(owner)).thenReturn(List.of());
            when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(inv -> inv.getArgument(0));

            FoodItem result = foodItemService.createFoodItem(input, owner);

            assertNotNull(result);
            assertEquals("Paneer Roll", result.getName());
            verify(foodItemRepository).save(any(FoodItem.class));
        }

        @Test
        @DisplayName("should throw when a duplicate active name exists for the same user")
        void testCreate_DuplicateName() {
            FoodItem input = FoodItem.builder().name("Samosa").price(BigDecimal.TEN).build();
            when(foodItemRepository.findAllByUser(owner)).thenReturn(List.of(existingItem));

            assertThrows(RuntimeException.class, () -> foodItemService.createFoodItem(input, owner));
        }

        @Test
        @SuppressWarnings("null")
        @DisplayName("should re-activate and update an inactive item with the same name")
        void testCreate_ReactivatesInactive() {
            existingItem.setIsActive(false);
            FoodItem input = FoodItem.builder().name("Samosa").price(new BigDecimal("20.00")).build();

            when(foodItemRepository.findAllByUser(owner)).thenReturn(List.of(existingItem));
            when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(inv -> inv.getArgument(0));

            FoodItem result = foodItemService.createFoodItem(input, owner);

            assertTrue(result.getIsActive());
            assertEquals(new BigDecimal("20.00"), result.getPrice());
        }

        @Test
        @DisplayName("should throw when input is null")
        void testCreate_NullInput() {
            assertThrows(IllegalArgumentException.class,
                    () -> foodItemService.createFoodItem(null, owner));
        }

        @Test
        @DisplayName("should throw when user is null")
        void testCreate_NullUser() {
            FoodItem input = FoodItem.builder().name("Test").price(BigDecimal.TEN).build();
            assertThrows(IllegalArgumentException.class,
                    () -> foodItemService.createFoodItem(input, null));
        }
    }

    // ─────────────────────── DELETE ───────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should soft-delete item owned by user")
        void testDelete_OwnerSuccess() {
            when(foodItemRepository.findById(10)).thenReturn(Optional.of(existingItem));
            when(foodItemRepository.save(any(FoodItem.class))).thenReturn(existingItem);

            foodItemService.delete(10, owner);

            assertFalse(existingItem.getIsActive());
            verify(foodItemRepository).save(existingItem);
        }

        @Test
        @SuppressWarnings("null")
        @DisplayName("should allow super admin to delete any item")
        void testDelete_SuperAdminSuccess() {
            when(foodItemRepository.findById(10)).thenReturn(Optional.of(existingItem));
            when(foodItemRepository.save(any(FoodItem.class))).thenReturn(existingItem);

            foodItemService.delete(10, superAdmin);

            assertFalse(existingItem.getIsActive());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when non-owner tries to delete")
        void testDelete_AccessDenied() {
            when(foodItemRepository.findById(10)).thenReturn(Optional.of(existingItem));
            assertThrows(AccessDeniedException.class,
                    () -> foodItemService.delete(10, anotherUser));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException for missing item")
        void testDelete_NotFound() {
            when(foodItemRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> foodItemService.delete(999, owner));
        }
    }

    // ─────────────────────── UPDATE ───────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @SuppressWarnings("null")
        @DisplayName("should update item when owner calls update")
        void testUpdate_OwnerSuccess() {
            FoodItem input = FoodItem.builder().name("Big Samosa").price(new BigDecimal("25.00")).build();

            when(foodItemRepository.findById(10)).thenReturn(Optional.of(existingItem));
            when(foodItemRepository.save(any(FoodItem.class))).thenAnswer(inv -> inv.getArgument(0));

            FoodItem result = foodItemService.update(10, input, owner);

            assertEquals("Big Samosa", result.getName());
            assertEquals(new BigDecimal("25.00"), result.getPrice());
        }

        @Test
        @DisplayName("should deny update from non-owner")
        void testUpdate_AccessDenied() {
            FoodItem input = FoodItem.builder().name("X").price(BigDecimal.ONE).build();
            when(foodItemRepository.findById(10)).thenReturn(Optional.of(existingItem));

            assertThrows(AccessDeniedException.class,
                    () -> foodItemService.update(10, input, anotherUser));
        }

        @Test
        @DisplayName("should throw for missing item on update")
        void testUpdate_NotFound() {
            FoodItem input = FoodItem.builder().name("X").price(BigDecimal.ONE).build();
            when(foodItemRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> foodItemService.update(999, input, owner));
        }
    }
}
