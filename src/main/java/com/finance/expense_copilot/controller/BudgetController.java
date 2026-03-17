package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.Expense;
import com.finance.expense_copilot.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping("/status")
    public Map<String, Object> getBudgetStatus(@RequestHeader("userEmail") String userEmail, @RequestParam(defaultValue = "50000.0") Double totalBudget) {
        List<Expense> allExpenses = expenseRepository.findByUserEmail(userEmail);
        
        // Sum all transactions that are genuinely EXPENSE
        double totalSpent = allExpenses.stream()
                .filter(e -> "EXPENSE".equalsIgnoreCase(e.getType()))
                .mapToDouble(e -> e.getAmount() != null ? e.getAmount() : 0.0)
                .sum();
                
        boolean exceeds80Percent = totalSpent >= (totalBudget * 0.8);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalBudget", totalBudget);
        response.put("totalSpent", totalSpent);
        response.put("remaining", totalBudget - totalSpent);
        response.put("exceeds80Percent", exceeds80Percent);
        
        return response;
    }
}
