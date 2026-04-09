package com.invoice.auth.repository;

import com.invoice.auth.entity.Driver;
import com.invoice.auth.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DriverRepository extends CrudRepository<Driver, Integer> {
    Optional<Driver> findByUser(User user);
    Optional<Driver> findByUserId(Integer userId);
    java.util.List<Driver> findByUserParentUser(User parentUser);
}
