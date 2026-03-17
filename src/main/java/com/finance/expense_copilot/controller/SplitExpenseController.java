package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.SplitExpense;
import com.finance.expense_copilot.repository.SplitExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/split")
public class SplitExpenseController {

    @Autowired
    private SplitExpenseRepository splitExpenseRepository;

    @PostMapping("/add")
    public SplitExpense recordSplitExpense(@RequestHeader("userEmail") String userEmail, @RequestBody SplitExpense splitExpense) {
        splitExpense.setUserEmail(userEmail);
        return splitExpenseRepository.save(splitExpense);
    }

    @GetMapping("/all")
    public List<SplitExpense> getAllSplitExpenses(@RequestHeader("userEmail") String userEmail) {
        return splitExpenseRepository.findByUserEmail(userEmail);
    }
    
    @PutMapping("/{id}/settle/{personName}")
    public SplitExpense settlePerson(@RequestHeader("userEmail") String userEmail, @PathVariable String id, @PathVariable String personName) {
        return splitExpenseRepository.findById(id).map(split -> {
            boolean settledSomeone = false;
            for (SplitExpense.SplitDetail detail : split.getSplitDetails()) {
                if (detail.getPerson().equalsIgnoreCase(personName)) {
                    detail.setSettled(true);
                    settledSomeone = true;
                }
            }
            if (!settledSomeone) throw new RuntimeException("Person not found in this split expense.");
            return splitExpenseRepository.save(split);
        }).orElseThrow(() -> new RuntimeException("Split expense not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteSplitExpense(@RequestHeader("userEmail") String userEmail, @PathVariable String id) {
        splitExpenseRepository.deleteById(id);
    }
}
