package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.dto.ExpenseDTO;
import com.finance.expense_copilot.model.Expense;
import com.finance.expense_copilot.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService service;

    @PostMapping("/add")
    public Expense addExpense(@RequestHeader("userEmail") String email, @RequestBody ExpenseDTO dto) {
        return service.addExpense(email, dto);
    }

    @GetMapping("/all")
    public List<Expense> getAll(@RequestHeader("userEmail") String email) {
        return service.getAll(email);
    }

    @PutMapping("/{id}")
    public Expense update(@PathVariable String id, @RequestBody ExpenseDTO dto) {
        return service.updateExpense(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@RequestHeader("userEmail") String email, @PathVariable String id) {
        service.deleteExpense(email, id);
    }

    @GetMapping("/user/balance/{email}")
    public Double balance(@PathVariable String email) {
        return service.getBalance(email);
    }

    @GetMapping("/user/{email}/daily-aggregated")
    public Map<String, Double> daily(@PathVariable String email) {
        return service.getDailyAggregated(email);
    }

    @DeleteMapping("/reset")
    public void reset(@RequestHeader("userEmail") String email) {
        service.reset(email);
    }

    @PostMapping("/scan-bill")
    public ResponseEntity<Expense> scan(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.mockOCR(file.getOriginalFilename()));
    }
}