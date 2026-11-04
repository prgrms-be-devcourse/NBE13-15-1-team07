package com.team7.cafe.category.repository;
import com.team7.cafe.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);
    List<Category> findAllByOrderByIdAsc();
}
