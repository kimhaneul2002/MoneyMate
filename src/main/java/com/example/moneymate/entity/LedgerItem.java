package com.example.moneymate.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 카테고리 테이블과 연결되는 Entity
@Entity
public class LedgerItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id; // 각 내역별 식별 고유ID

    // @NotBlank 조건정의
    @NotBlank(message = "내용을 적어주세요.")
    String title; // 내용

    // @Min 조건정의 (amount는 최소 1 이상)
    @Min(value=1,message="금액은 0원 이상이어야 합니다.")
    // @NotBlank 조건정의
    @NotNull(message = "금액을 적어주세요.")
    Long amount; // 금액

    // @NotBlank 조건정의
    @NotNull(message = "거래일자는 필수입니다.")
    LocalDate transactionDate; // 거래일자

    @CreationTimestamp
    LocalDateTime createdAt; // 생성일시

    @UpdateTimestamp
    LocalDateTime updatedAt; // 수정일시

    @ManyToOne // 다대일
    @JoinColumn(name = "user_id") // 열 추가
    User user; // 유저타입의 유저 선언

    @ManyToOne // 다대일
    @JoinColumn(name = "category_id") // 열 추가
    Category category; // 카테고리타입의 카테고리 선언

    // 생성자
    LedgerItem(String title, Long amount, LocalDate transactionDate, LocalDateTime createdAt, LocalDateTime updatedAt){
        this.title=title;
        this.amount=amount;
        this.transactionDate=transactionDate;
        this.createdAt=createdAt;
        this.updatedAt=updatedAt;
    }

    // JSON을 객체로 변환할때 값 없이 객체를 먼저 만들 수 있는 방법인 빈 생성자 필요
    LedgerItem(){
    }

    // Getter 메서드
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public Long getAmount() { return amount; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }

    // Setter 메서드
    public void setTitle(String title) { this.title = title; }
    public void setAmount(Long amount) { this.amount = amount; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setCategory(Category category) { this.category = category; }
}