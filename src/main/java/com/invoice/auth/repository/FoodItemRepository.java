package com.invoice.auth.repository;

import com.invoice.auth.entity.FoodItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends CrudRepository<FoodItem, Integer> {
    List<FoodItem> findByUserAndIsActiveTrue(com.invoice.auth.entity.User user);

    List<FoodItem> findByIsActiveTrue();
}
