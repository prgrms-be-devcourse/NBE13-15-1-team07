package com.team7.cafe.category.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.team7.cafe.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
}
