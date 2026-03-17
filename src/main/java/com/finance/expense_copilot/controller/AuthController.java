package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.AuthRequest;
import com.finance.expense_copilot.model.AuthResponse;
import com.finance.expense_copilot.model.OtpVerifyRequest;
import com.finance.expense_copilot.model.User;
import com.finance.expense_copilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // In-memory storage (clears when server restarts)
    private Map<String, String> otpStorage = new HashMap<>();

    @PostMapping("/send-otp")
    public AuthResponse sendOtp(@RequestBody AuthRequest request) {
        String email = request.getEmail();
        // Generate 6-digit OTP
        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        otpStorage.put(email, otp);

        // Check your IntelliJ console for this code!
        System.out.println("DEBUG >>> OTP for " + email + " is: " + otp);

        // For testing purposes, we return the OTP in the message so the user can copy it from the Toast
        return new AuthResponse(true, "Your OTP is: " + otp);
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@RequestBody OtpVerifyRequest request) {
        String savedOtp = otpStorage.get(request.getEmail());

        if (savedOtp != null && savedOtp.equals(request.getOtp())) {
            otpStorage.remove(request.getEmail()); // Clean up
            
            // Register user if they don't exist
            User user = userRepository.findByEmail(request.getEmail());
            if (user == null) {
                user = new User(request.getEmail());
                userRepository.save(user);
            }
            
            // Generate Session Token
            String token = UUID.randomUUID().toString();
            
            return new AuthResponse(true, "Login Successful", token);
        } else {
            return new AuthResponse(false, "Invalid OTP", null);
        }
    }
}