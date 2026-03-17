package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.Reminder;
import com.finance.expense_copilot.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    @Autowired
    private ReminderRepository repository;

    @GetMapping("/all")
    public List<Reminder> getUpcomingReminders(@RequestHeader("userEmail") String userEmail) {
        return repository.findByUserEmail(userEmail);
    }
    
    @PostMapping("/add")
    public Reminder addReminder(@RequestHeader("userEmail") String userEmail, @RequestBody Reminder reminder) {
        reminder.setUserEmail(userEmail);
        return repository.save(reminder);
    }
    
    @DeleteMapping("/{id}")
    public void deleteReminder(@RequestHeader("userEmail") String userEmail, @PathVariable String id) {
        repository.deleteById(id);
    }
}
