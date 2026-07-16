package com.example.moneymate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.moneymate.entity.LedgerItem;
import java.time.LocalDate;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerItem, Long> { // LedgerItem엔티티를 다루고, 해당 엔티티의 ID타입
    // 월별 내역 조회 시 사용할 메서드 (= 두 매개변수 사이 내용을 거래일자기준으로 내림차순으로 정렬하는 메서드)
    // 메서드 이름과 타입을 규칙에 맞게 써주고 Jpa가 SQL만들도록 메서드 선언
    List<LedgerItem> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDate start, LocalDate end);
}


// => 기존에 있는 JpaReposiotry 기능세트를 상속받는 인터페이스 틀을 사용
//    여기에 조회,검색,삭제 등 기능이 다 있으니까 내용은 없어도됨