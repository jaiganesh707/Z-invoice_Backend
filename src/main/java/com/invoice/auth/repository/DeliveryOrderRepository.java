package com.invoice.auth.repository;

import com.invoice.auth.entity.DeliveryOrder;
import com.invoice.auth.entity.Driver;
import com.invoice.auth.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeliveryOrderRepository extends CrudRepository<DeliveryOrder, Integer> {
    List<DeliveryOrder> findByCustomer(User customer);
    List<DeliveryOrder> findByDriver(Driver driver);
    List<DeliveryOrder> findByStatus(com.invoice.auth.entity.DeliveryStatus status);
}
