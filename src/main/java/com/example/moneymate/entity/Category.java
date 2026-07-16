// 카테고리 테이블 파일
package com.example.moneymate.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// 카테고리 테이블과 연결되는 Entity
@Entity
public class Category {

    @Id // 식별 고유 번호 사용 의미
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 번호 자동으로 1,2,3 순서대로 매겨줌 의미
    Long id; // 각 카테고리별 식별 고유ID

    String name; // 각 카테고리별 이름

    // JPA에게 숫자말고 enum이름을 문자열로 조회/저장하는 애너테이션
    @Enumerated(EnumType.STRING)
    CategoryType type; // 지출,수입 구분 타입 (enum 클래스 타입)

    // 생성자
    Category(String name, CategoryType type){
        this.name=name;
        this.type=type;
    }

    // JSON을 객체로 변환할때 값 없이 객체를 먼저 만들 수 있는 방법인 빈 생성자 필요
    Category() {
    }

    // Getter 메서드 - 고유 번호 Id 가져오기
    public Long getId(){
        return id;
    }

    // Getter 메서드 - 이름,타입 가져오기
    public String getName(){
        return name;
    }
    public CategoryType getType(){
        return type;
    }

    // Setter 메서드 - 타입 바꾸기
    public void setType(CategoryType type) {
        this.type = type;
    }
}