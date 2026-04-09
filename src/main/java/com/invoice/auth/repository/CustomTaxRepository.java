package com.invoice.auth.repository;

import com.invoice.auth.entity.CustomTax;
import com.invoice.auth.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTaxRepository extends CrudRepository<CustomTax, Integer> {
    List<CustomTax> findAllByUser(User user);
    List<CustomTax> findAllByUserAndIsActiveTrue(User user);
}
