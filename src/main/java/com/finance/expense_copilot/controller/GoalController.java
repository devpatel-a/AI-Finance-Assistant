package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.Goal;
import com.finance.expense_copilot.repository.GoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.finance.expense_copilot.repository.ExpenseRepository;
import com.finance.expense_copilot.model.Expense;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@CrossOrigin(origins = "*") // Allows the Android Emulator to connect
public class GoalController {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    // 1. Fetch all goals for the Android History List
    @GetMapping("/all")
    public List<Goal> getAllGoals(@RequestHeader("userEmail") String userEmail) {
        return goalRepository.findByUserEmail(userEmail);
    }

    // 2. Add a new goal (e.g., "New Laptop")
    @PostMapping("/add")
    public Goal addGoal(@RequestHeader("userEmail") String userEmail, @RequestBody Goal goal) {
        goal.setUserEmail(userEmail);
        if (goal.getCurrentSaved() == null) {
            goal.setCurrentSaved(0.0);
        }
        return goalRepository.save(goal);
    }

    // 3. Update savings (Add money to a specific goal)
    @PutMapping("/{id}/contribute")
    public Goal contributeToGoal(@RequestHeader("userEmail") String userEmail, @PathVariable String id, @RequestParam double amount) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        goal.setCurrentSaved(goal.getCurrentSaved() + amount);
        return goalRepository.save(goal);
    }
    
    // 4. AI-based spending cut suggestions
    @GetMapping("/suggest-cuts")
    public Map<String, Object> suggestCuts(@RequestHeader("userEmail") String userEmail, @RequestParam double targetSavings) {
        List<Expense> expenses = expenseRepository.findByUserEmail(userEmail);
        
        // Flexible categories that can be cut
        List<String> flexibleCategories = List.of("Entertainment", "Shopping", "Food", "Travel");
        
        double totalFlexibleSpend = 0.0;
        Map<String, Double> categorySpend = new HashMap<>();
        
        for (Expense e : expenses) {
            if ("EXPENSE".equalsIgnoreCase(e.getType()) && flexibleCategories.contains(e.getCategory())) {
                double amt = e.getAmount() != null ? e.getAmount() : 0.0;
                totalFlexibleSpend += amt;
                categorySpend.put(e.getCategory(), categorySpend.getOrDefault(e.getCategory(), 0.0) + amt);
            }
        }
        
        Map<String, Object> suggestions = new HashMap<>();
        suggestions.put("targetToSave", targetSavings);
        suggestions.put("totalFlexibleSpend", totalFlexibleSpend);
        
        if (totalFlexibleSpend == 0) {
            suggestions.put("message", "No flexible spending found to cut.");
            return suggestions;
        }

        Map<String, Double> recommendedCuts = new HashMap<>();
        
        // Generate Fintastics Style AI Tips
        java.util.List<Map.Entry<String, Double>> sortedCategories = new java.util.ArrayList<>(categorySpend.entrySet());
        sortedCategories.sort((a, b) -> b.getValue().compareTo(a.getValue())); // Descending order
        
        List<String> aiTips = new java.util.ArrayList<>();
        int limit = Math.min(3, sortedCategories.size());
        
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            double proportion = (entry.getValue() / totalFlexibleSpend) * 100;
            String promptStyleTip = String.format("You spent %d%% on %s; consider cutting this back to save ₹%d.", 
                                                  (int)proportion, entry.getKey(), (int)(entry.getValue() * 0.2)); // Suggest 20% cut
            aiTips.add(promptStyleTip);
            recommendedCuts.put(entry.getKey(), entry.getValue() * 0.2); // Store actual cut data too
        }

        if (totalFlexibleSpend >= targetSavings) {
            suggestions.put("status", "Possible");
            suggestions.put("action", "Based on your highest spending, here are 3 quick actions you can take today:");
        } else {
            suggestions.put("status", "Challenging");
            suggestions.put("action", "It will be hard to meet your goal, but start by reducing these categories immediately:");
        }
        
        suggestions.put("cuts", recommendedCuts);
        suggestions.put("aiTips", aiTips); // New property for frontend to display
        return suggestions;
    }
}