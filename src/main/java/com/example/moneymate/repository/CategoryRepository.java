package com.example.moneymate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.moneymate.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}


// => 기존에 있는 JpaReposiotry 기능세트를 상속받는 인터페이스 틀을 사용
//    여기에 조회,검색,삭제 등 기능이 다 있으니까 내용은 없어도됨