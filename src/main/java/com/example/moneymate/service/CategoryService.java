package com.example.moneymate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.moneymate.entity.Category;
import com.example.moneymate.repository.CategoryRepository;
import java.util.List;

@Service
public class CategoryService {

    // CategoryRepository타입 변수를 선언해서 AutoWired로 객체 생성해서 Repository에 연결
    @Autowired
    private CategoryRepository categoryRepository;

    // 전체 카테고리 목록 조회
    // 카테고리타입의 데이터를 여러개 담은 리스트형태로 반환
    public List<Category> getAllCategories(){
        return categoryRepository.findAll(); // 레포지토리에 전체 조회 요청
    }
}