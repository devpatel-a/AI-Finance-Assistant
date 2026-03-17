package com.finance.expense_copilot.service;

import com.finance.expense_copilot.dto.ExpenseDTO;
import com.finance.expense_copilot.model.Expense;
import com.finance.expense_copilot.model.User;
import com.finance.expense_copilot.repository.ExpenseRepository;
import com.finance.expense_copilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository repository;

    @Autowired
    private UserRepository userRepository;

    public Expense addExpense(String userEmail, ExpenseDTO dto) {
        Expense expense = new Expense();

        expense.setUserEmail(userEmail);
        expense.setDescription(dto.getDescription());
        expense.setAmount(dto.getAmount());
        expense.setCategory(dto.getCategory() != null ? dto.getCategory() : "Other");
        expense.setType(dto.getType());
        expense.setDate(dto.getDate());
        expense.setAccountType(dto.getAccountType());

        Expense saved = repository.save(expense);

        updateUserBalance(userEmail, dto.getAmount(), dto.getType(), dto.getAccountType(), true);

        return saved;
    }

    public List<Expense> getAll(String userEmail) {
        return repository.findByUserEmail(userEmail);
    }

    public Expense updateExpense(String id, ExpenseDTO dto) {
        return repository.findById(id).map(expense -> {
            expense.setDescription(dto.getDescription());
            expense.setAmount(dto.getAmount());
            expense.setCategory(dto.getCategory());
            expense.setType(dto.getType());
            expense.setDate(dto.getDate());
            return repository.save(expense);
        }).orElseThrow(() -> new RuntimeException("Expense not found"));
    }

    public void deleteExpense(String userEmail, String id) {
        repository.findById(id).ifPresent(expense -> {
            updateUserBalance(userEmail, expense.getAmount(), expense.getType(), expense.getAccountType(), false);
            repository.deleteById(id);
        });
    }

    public Double getBalance(String email) {
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

    public Map<String, Double> getDailyAggregated(String email) {
        List<Expense> expenses = repository.findByUserEmail(email);
        Map<String, Double> map = new TreeMap<>();

        for (Expense e : expenses) {
            if ("EXPENSE".equalsIgnoreCase(e.getType()) && e.getDate() != null && e.getAmount() != null) {
                map.put(e.getDate(), map.getOrDefault(e.getDate(), 0.0) + e.getAmount());
            }
        }
        return map;
    }

    public void reset(String userEmail) {
        repository.deleteAll(repository.findByUserEmail(userEmail));
    }

    public Expense mockOCR(String fileName) {
        Expense e = new Expense();
        e.setDescription("Mock OCR " + fileName);
        e.setAmount(499.0);
        e.setCategory("Shopping");
        e.setType("EXPENSE");
        return e;
    }

    private void updateUserBalance(String email, Double amount, String type, String accountType, boolean isAdding) {

        if (amount == null || accountType == null) return;

        User user = userRepository.findByEmail(email);
        if (user == null) return;

        boolean isIncome = "INCOME".equalsIgnoreCase(type);
        if (!isAdding) isIncome = !isIncome;

        if ("Cash".equalsIgnoreCase(accountType)) {
            user.setCashBalance(isIncome ? user.getCashBalance() + amount : user.getCashBalance() - amount);
        } else if ("Bank".equalsIgnoreCase(accountType)) {
            user.setBankBalance(isIncome ? user.getBankBalance() + amount : user.getBankBalance() - amount);
        }

        userRepository.save(user);
    }
}