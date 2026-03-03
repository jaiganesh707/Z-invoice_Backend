package com.invoice.auth.service;

import com.invoice.auth.exception.ResourceNotFoundException;

import com.invoice.auth.entity.FoodItem;
import com.invoice.auth.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FoodItemService {
    private final FoodItemRepository foodItemRepository;

    public List<FoodItem> allFoodItems() {
        return new ArrayList<>(foodItemRepository.findByIsActiveTrue());
    }

    @SuppressWarnings("null")
    public List<FoodItem> getFoodItemsByUser(com.invoice.auth.entity.User user) {
        return foodItemRepository.findByUserAndIsActiveTrue(user);
    }

    @SuppressWarnings("null")
    public FoodItem createFoodItem(FoodItem input, com.invoice.auth.entity.User user) {
        if (input == null || input.getName() == null || input.getPrice() == null || user == null) {
            throw new IllegalArgumentException("Food item name, price, and user must not be null");
        }
        input.setUser(user);
        return foodItemRepository.save(input);
    }

    @SuppressWarnings("null")
    public Optional<FoodItem> findById(Integer id) {
        // Also ensure we only return it if it is active.
        return foodItemRepository.findById(id).filter(FoodItem::getIsActive);
    }

    @SuppressWarnings("null")
    public void delete(Integer id, com.invoice.auth.entity.User currentUser) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));

        if (currentUser.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN &&
                !foodItem.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: You can only delete your own items");
        }

        foodItem.setIsActive(false);
        foodItemRepository.save(foodItem);
    }

    @SuppressWarnings("null")
    public FoodItem update(Integer id, FoodItem input, com.invoice.auth.entity.User currentUser) {
        if (id == null || input == null) {
            throw new IllegalArgumentException("ID and input data must not be null");
        }
        return foodItemRepository.findById(id).map(foodItem -> {
            if (currentUser.getRole() != com.invoice.auth.entity.RoleEnum.ROLE_SUPER_ADMIN &&
                    !foodItem.getUser().getId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: You can only update your own items");
            }
            foodItem.setName(input.getName());
            foodItem.setPrice(input.getPrice());
            foodItem.setDescription(input.getDescription());
            return foodItemRepository.save(foodItem);
        }).orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
    }
}
