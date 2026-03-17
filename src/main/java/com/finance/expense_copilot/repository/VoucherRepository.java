package com.finance.expense_copilot.repository;

import com.finance.expense_copilot.model.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VoucherRepository extends MongoRepository<Voucher, String> {
    java.util.List<Voucher> findByUserEmail(String userEmail);
}
