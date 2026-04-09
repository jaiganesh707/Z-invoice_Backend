package com.invoice.auth.repository;

import com.invoice.auth.entity.BusinessAsset;
import com.invoice.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessAssetRepository extends JpaRepository<BusinessAsset, Integer> {
    List<BusinessAsset> findAllByUserOrderByCreatedAtDesc(User user);
    List<BusinessAsset> findAllByUserInOrderByCreatedAtDesc(List<User> users);
}
