package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.User;
import com.finance.expense_copilot.repository.UserRepository;
import com.finance.expense_copilot.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public String login(@RequestBody User user) {

        User existing = userRepository.findByEmail(user.getEmail());

        if (existing != null && existing.getPassword().equals(user.getPassword())) {
            return jwtUtil.generateToken(user.getEmail());
        }

        throw new RuntimeException("Invalid credentials");
    }
}