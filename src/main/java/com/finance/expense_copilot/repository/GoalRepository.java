package com.finance.expense_copilot.repository;

import com.finance.expense_copilot.model.Goal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalRepository extends MongoRepository<Goal, String> {
    java.util.List<Goal> findByUserEmail(String userEmail);
}