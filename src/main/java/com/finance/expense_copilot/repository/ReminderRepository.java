package com.finance.expense_copilot.repository;

import com.finance.expense_copilot.model.Reminder;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReminderRepository extends MongoRepository<Reminder, String> {
    java.util.List<Reminder> findByUserEmail(String userEmail);
}
