// 유저 테이블 파일
package com.example.moneymate.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// 유저 테이블과 연결되는 Entity
@Entity
public class User {

    @Id // 식별 고유 번호 사용 의미
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 번호 자동으로 1,2,3 순서대로 매겨줌 의미
    Long id; // 각 유저별 식별 고유ID

    String username; // 각 유저별 이름

    // 생성자
    User(String username){
        this.username=username;
    }

    // JSON을 객체로 변환할때 값 없이 객체를 먼저 만들 수 있는 방법인 빈 생성자 필요
    User() {
    }

    // Getter 메서드 - 고유 번호 Id 가져오기
    public Long getId(){
        return id;
    }

    // Getter 메서드 - 이름,타입 가져오기
    public String getUsername(){
        return username;
    }

    // Setter 메서드 - 타입 바꾸기
    public void setUsername(String username) {
        this.username = username;
    }
}