package com.finance.expense_copilot.repository;

import com.finance.expense_copilot.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {
    java.util.List<Expense> findByUserEmail(String userEmail);
}