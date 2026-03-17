package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.Expense;
import com.finance.expense_copilot.model.User;
import com.finance.expense_copilot.repository.ExpenseRepository;
import com.finance.expense_copilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add")
    public Expense addExpense(@RequestHeader("userEmail") String userEmail, @RequestBody Expense expense) {
        expense.setUserEmail(userEmail);

        // Retain the category explicitly sent from the frontend Fintastics UI. 
        // Only default to "Other" if it was somehow skipped.
        if (expense.getCategory() == null || expense.getCategory().trim().isEmpty()) {
            expense.setCategory("Other");
        }

        Expense savedExpense = repository.save(expense);
        
        // Auto-update Cash/Bank balances if provided
        updateUserBalance(userEmail, expense.getAmount(), expense.getType(), expense.getAccountType(), true);

        return savedExpense;
    }

    @GetMapping("/all")
    public List<Expense> getAll(@RequestHeader("userEmail") String userEmail) {
        return repository.findByUserEmail(userEmail);
    }
    
    @PutMapping("/{id}")
    public Expense updateExpense(@RequestHeader("userEmail") String userEmail, @PathVariable String id, @RequestBody Expense updatedExpense) {
        return repository.findById(id).map(expense -> {
            expense.setDescription(updatedExpense.getDescription());
            expense.setAmount(updatedExpense.getAmount());
            expense.setCategory(updatedExpense.getCategory());
            expense.setType(updatedExpense.getType());
            expense.setDate(updatedExpense.getDate());
            return repository.save(expense);
        }).orElseThrow(() -> new RuntimeException("Expense not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@RequestHeader("userEmail") String userEmail, @PathVariable String id) {
        repository.findById(id).ifPresent(expense -> {
            // Reverse the balance impact Before deleting
            updateUserBalance(userEmail, expense.getAmount(), expense.getType(), expense.getAccountType(), false);
            repository.deleteById(id);
        });
    }
    
    @GetMapping("/user/balance/{email}")
    public Double getUserBalance(@PathVariable String email) {
        List<Expense> expenses = repository.findByUserEmail(email);
        double income = expenses.stream()
                .filter(e -> "INCOME".equalsIgnoreCase(e.getType()))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();
        double expense = expenses.stream()
                .filter(e -> "EXPENSE".equalsIgnoreCase(e.getType()))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();
        return income - expense;
    }
    
    @GetMapping("/user/{email}/daily-aggregated")
    public java.util.Map<String, Double> getDailyAggregated(@PathVariable String email) {
        List<Expense> expenses = repository.findByUserEmail(email);
        java.util.Map<String, Double> dailyData = new java.util.TreeMap<>(); // TreeMap to keep dates sorted
        
        for (Expense e : expenses) {
            if ("EXPENSE".equalsIgnoreCase(e.getType()) && e.getDate() != null && e.getAmount() != null) {
                dailyData.put(e.getDate(), dailyData.getOrDefault(e.getDate(), 0.0) + e.getAmount());
            }
        }
        return dailyData;
    }
    
    @DeleteMapping("/reset")
    public void resetMonthlyExpenses(@RequestHeader("userEmail") String userEmail) {
        List<Expense> toDelete = repository.findByUserEmail(userEmail);
        repository.deleteAll(toDelete);
    }
    
    @PostMapping("/scan-bill")
    public ResponseEntity<Expense> scanBillOCR(@RequestParam("file") MultipartFile file) {
        // Mock OCR parsing logic simulating a Vision API
        Expense mockedParsedExpense = new Expense();
        mockedParsedExpense.setDescription("Mocked OCR Scan " + file.getOriginalFilename());
        mockedParsedExpense.setAmount(499.0);
        mockedParsedExpense.setCategory("Shopping");
        mockedParsedExpense.setType("EXPENSE");
        
        return ResponseEntity.ok(mockedParsedExpense);
    }
    
    // Helper to adjust User balances based on transactions
    private void updateUserBalance(String email, Double amount, String type, String accountType, boolean isAdding) {
        if (amount == null || amount == 0.0 || accountType == null) return;
        
        User user = userRepository.findByEmail(email);
        if (user == null) return;
        
        // If we are deleting an expense, reverse the math
        boolean isIncomeAction = "INCOME".equalsIgnoreCase(type);
        if (!isAdding) {
            isIncomeAction = !isIncomeAction; 
        }
        
        if ("Cash".equalsIgnoreCase(accountType)) {
            double current = user.getCashBalance();
            user.setCashBalance(isIncomeAction ? current + amount : current - amount);
        } else if ("Bank".equalsIgnoreCase(accountType)) {
            double current = user.getBankBalance();
            user.setBankBalance(isIncomeAction ? current + amount : current - amount);
        }
        
        userRepository.save(user);
    }
}