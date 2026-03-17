package com.finance.expense_copilot.controller;

import com.finance.expense_copilot.model.Voucher;
import com.finance.expense_copilot.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    private VoucherRepository voucherRepository;

    @PostMapping("/add")
    public Voucher addVoucher(@RequestHeader("userEmail") String userEmail, @RequestBody Voucher voucher) {
        voucher.setUserEmail(userEmail);
        return voucherRepository.save(voucher);
    }

    @GetMapping("/all")
    public List<Voucher> getAllVouchers(@RequestHeader("userEmail") String userEmail) {
        return voucherRepository.findByUserEmail(userEmail);
    }

    @DeleteMapping("/{id}")
    public void deleteVoucher(@RequestHeader("userEmail") String userEmail, @PathVariable String id) {
        voucherRepository.deleteById(id);
    }
}
