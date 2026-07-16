package com.example.moneymate.dto;

import com.example.moneymate.entity.LedgerItem;
import java.util.List;

public class LedgerMonthlyResponse {

    private Long totalIncome;   // 총 수입
    private Long totalExpense;  // 총 지출
    private List<LedgerItem> items; // 거래 내역 목록

    // 생성자
    public LedgerMonthlyResponse(Long totalIncome, Long totalExpense, List<LedgerItem> items) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.items = items;
    }
    // Getter
    public Long getTotalIncome() { return totalIncome; }
    public Long getTotalExpense() { return totalExpense; }
    public List<LedgerItem> getItems() { return items; }
}