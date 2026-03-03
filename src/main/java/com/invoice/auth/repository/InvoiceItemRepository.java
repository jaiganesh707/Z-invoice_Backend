package com.invoice.auth.repository;

import com.invoice.auth.entity.InvoiceItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceItemRepository extends CrudRepository<InvoiceItem, Integer> {
}
