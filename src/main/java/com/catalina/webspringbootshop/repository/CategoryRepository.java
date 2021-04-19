package com.catalina.webspringbootshop.repository;

import com.catalina.webspringbootshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    public Category findById(int id);
}