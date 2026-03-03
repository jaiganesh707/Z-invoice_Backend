package com.invoice.auth.repository;

import com.invoice.auth.entity.Invoice;
import com.invoice.auth.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends CrudRepository<Invoice, Integer> {
    List<Invoice> findAllByUserOrderByCreatedAtDesc(User user);

    List<Invoice> findAllByOrderByCreatedAtDesc();
}
