package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.dto.ExpenseDTO;
import com.finance.expense_copilot.model.Expense;
import com.finance.expense_copilot.service.ExpenseService;
import jakarta.servlet.http.HttpServletRequest;
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
    public Expense addExpense(HttpServletRequest request, @RequestBody ExpenseDTO dto) {
        String email = (String) request.getAttribute("userEmail");
        return service.addExpense(email, dto);
    }

    @GetMapping("/all")
    public List<Expense> getAll(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        return service.getAll(email);
    }

    @PutMapping("/{id}")
    public Expense update(@PathVariable String id, @RequestBody ExpenseDTO dto) {
        return service.updateExpense(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest request, @PathVariable String id) {
        String email = (String) request.getAttribute("userEmail");
        service.deleteExpense(email, id);
    }

    @GetMapping("/user/balance")
    public Double balance(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        return service.getBalance(email);
    }

    @GetMapping("/user/daily-aggregated")
    public Map<String, Double> daily(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        return service.getDailyAggregated(email);
    }

    @DeleteMapping("/reset")
    public void reset(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        service.reset(email);
    }

    @PostMapping("/scan-bill")
    public ResponseEntity<Expense> scan(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.mockOCR(file.getOriginalFilename()));
    }
}