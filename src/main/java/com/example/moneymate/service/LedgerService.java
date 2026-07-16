package com.example.moneymate.service;

import com.example.moneymate.dto.LedgerMonthlyResponse;
import com.example.moneymate.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.moneymate.entity.LedgerItem;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.example.moneymate.exception.LedgerNotFoundException;
import com.example.moneymate.entity.CategoryType;

@Service
public class LedgerService {

    // LedgerRepository타입 변수를 선언해서 AutoWired로 객체 생성해서 Repository에 연결
    @Autowired
    private LedgerRepository ledgerRepository;

    // AiAnalysisService타입 변수를 선언해서 AutoWired로 객체 생성해서 Repository에 연결
    @Autowired
    private AiAnalysisService aiAnalysisService;

    // 거래 내역 등록
    public LedgerItem addLedger(LedgerItem ledgerItem) {
        return ledgerRepository.save(ledgerItem); // 레포지토리에 해당 LedgerItem 저장 요청
    }

    // 월별 내역 조회
    // 컨트롤러요청 -> 서비스파일에서 월별내역,총수입,총지출 구현해서 DTO에 담고 담은걸 반환
    public LedgerMonthlyResponse getLedgerByMonth(int year, int month) {

        // year년 month월 1일 날짜를 만들어줘
        LocalDate start = LocalDate.of(year, month, 1);

        // LengthOrMonth()로 이달이 며칠까지 있는지(마지막날) 알아내고, withDayOfMonth()로 마지막날로 바꿔줘서 저장
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        // 1. LedgerRepository에 저장된 월별 내역 목록 나열된걸 items 변수에 저장
        List<LedgerItem> items = ledgerRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);

        // 2. 총 수입,지출 합산
        Long totalIncome = 0L;
        Long totalExpense = 0L;
        for (LedgerItem item : items) {
            if (item.getCategory().getType() == CategoryType.INCOME) { // 이 내역의 카테고리의 타입 검사
                totalIncome += item.getAmount();
            } else {
                totalExpense += item.getAmount();
            }
        }

        // 3. DTO타입 객체 생성해서 담아서 반환
        return new LedgerMonthlyResponse(totalIncome, totalExpense, items);
    }

    // 거래 내역 수정
    public LedgerItem updateLedger(Long id, LedgerItem newItem) { // 바꿀내역 Id랑 바꾼 내역을 줘서 내역 수정 반환

        // Id로 기존 내역 찾기
        LedgerItem ledgerItem = ledgerRepository.findById(id).orElseThrow(() // id로 찾고 없으면 에러던짐
                -> new LedgerNotFoundException("내역을 찾을 수 없습니다."));

        // 값 바꾸기
        ledgerItem.setTitle(newItem.getTitle());
        ledgerItem.setAmount(newItem.getAmount());
        ledgerItem.setTransactionDate(newItem.getTransactionDate());
        ledgerItem.setCategory(newItem.getCategory());

        // 다시 저장
        return ledgerRepository.save(ledgerItem);
    }


    // 거래 내역 삭제
    public Map<String, String> deleteLedger(Long id){

        // Id로 삭제할 내역 찾기
        LedgerItem ledgerItem = ledgerRepository.findById(id).orElseThrow(() // id로 찾고 없으면 에러던짐
                -> new LedgerNotFoundException("내역을 찾을 수 없습니다."));

        // 내역 삭제
        ledgerRepository.deleteById(id);

        // Map 자료를 사용하여 JSON형식으로 반환
        Map<String, String> result = new HashMap<>(); // 해시맵 객체 생성
        result.put("message", "내역 삭제 완료");
        return result;
    }

    // AI 소비 분석 (AIAnalysisService 호출)
    public String getAiAnalysis(int year, int month) {
        // 월별 내역 조회의 dto안에 getItems메서드를 호출해서 items에 담음
        List<LedgerItem> items = getLedgerByMonth(year, month).getItems();
        // ai서비스로직의 analyze메서드 호출하여 반환
        return aiAnalysisService.analyze(items);
    }
}