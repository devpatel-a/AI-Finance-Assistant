package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.User;
import com.finance.expense_copilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{email}")
    public User getUser(@PathVariable String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            User newUser = new User(email);
            return userRepository.save(newUser);
        }
        return user;
    }

    @PutMapping("/{email}/settings")
    public User updateSettings(@PathVariable String email, @RequestBody User settings) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setNotificationsEnabled(settings.isNotificationsEnabled());
            user.setDailyLimit(settings.getDailyLimit());
            user.setAppLockEnabled(settings.isAppLockEnabled());
            user.setSmsTrackingEnabled(settings.isSmsTrackingEnabled());
            user.setCashBalance(settings.getCashBalance());
            user.setBankBalance(settings.getBankBalance());
            user.setCategoryLimits(settings.getCategoryLimits());
            return userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }
}
