package com.team7.cafe.menu.repository;

import com.team7.cafe.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    // [1] 전체 메뉴 ID 오름차순 (A06) [2026.11.02/안형빈]
    List<Menu> findAllByOrderByIdAsc();

    // [2] 특정 카테고리에 속한 메뉴 목록 ID 오름차순 (A15) [2026.11.02/안형빈]
    List<Menu> findByCategoryIdOrderByIdAsc(Long categoryId);

    // [3] 현재 판매 중(available = true)인 메뉴만 ID 오름차순 (A19) [2026.11.02/안형빈]
    List<Menu> findByAvailableTrueOrderByIdAsc();

    // [4] 해당 카테고리를 참조하는 메뉴가 존재하는지 확인 (카테고리 삭제 방지용) [2026.11.02/안형빈]
    boolean existsByCategoryId(Long categoryId);

    // [5] 메뉴명 검색 (A20: 대소문자 구분 및 와일드카드 특수문자 오작동 방지)
    // name 컬럼이 utf8mb4_0900_ai_ci라서 keyword에만 BINARY를 걸면 대소문자가 무시됨, 양쪽 모두 BINARY로 비교 [2026.11.03/안형빈]
    @Query(value = "SELECT * FROM menu WHERE LOCATE(BINARY :keyword, BINARY name) > 0 ORDER BY id", nativeQuery = true)
    List<Menu> search(@Param("keyword") String keyword);
}
