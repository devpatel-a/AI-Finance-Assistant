package com.finance.expense_copilot.repository;

import com.finance.expense_copilot.model.SplitExpense;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SplitExpenseRepository extends MongoRepository<SplitExpense, String> {
    java.util.List<SplitExpense> findByUserEmail(String userEmail);
}
