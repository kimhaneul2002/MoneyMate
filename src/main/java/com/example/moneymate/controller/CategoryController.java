package com.example.moneymate.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.example.moneymate.entity.Category;
import com.example.moneymate.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

// 이 클래스는 API요청을 처리하는 곳임을 알려주는 애너테이션
@RestController
public class CategoryController {

    // CategoryService타입 변수를 선언해서 AutoWired로 객체 생성해서 Service에 연결
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/api/categories")
    public List<Category> getAllCategories(){
        return categoryService.getAllCategories(); // 서비스의 전체목록조회 메서드 호출
    }
}