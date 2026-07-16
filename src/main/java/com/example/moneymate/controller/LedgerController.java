package com.example.moneymate.controller;

import com.example.moneymate.entity.LedgerItem;
import com.example.moneymate.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.moneymate.dto.LedgerMonthlyResponse;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
public class LedgerController {

    // CategoryService타입 변수를 선언해서 AutoWired로 객체 생성해서 Service에 연결
    @Autowired
    private LedgerService ledgerService;

    // 거래 내역 등록
    // @Valid로 조건검사, JSON형태로 내역 요청
    @PostMapping("/api/ledger")
    public LedgerItem addLedger(@Valid @RequestBody LedgerItem ledgerItem) {
        return ledgerService.addLedger(ledgerItem); // 서비스의 등록메서드 호출
    }

    // 월별 내역 조회
    @GetMapping("/api/ledger")
    public LedgerMonthlyResponse getLedgerByMonth(@RequestParam int year, @RequestParam int month){
        return ledgerService.getLedgerByMonth(year,month);
    }

    // 거래 내역 수정
    @PutMapping("/api/ledger/{id}")
    public LedgerItem updateLedger(@PathVariable Long id, @Valid @RequestBody LedgerItem newItem){
        return ledgerService.updateLedger(id,newItem);
    }

    // 거래 내역 삭제
    @DeleteMapping("/api/ledger/{id}")
    public Map<String, String> deleteLedger(@PathVariable Long id){
        return ledgerService.deleteLedger(id);
    }

    // AI 소비 분석
    @GetMapping("/api/ledger/ai-analysis")
    public String getAiAnalysis(@RequestParam int year, @RequestParam int month) {
        return ledgerService.getAiAnalysis(year, month);
    }
}