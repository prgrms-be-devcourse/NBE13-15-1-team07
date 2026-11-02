package com.team7.cafe.menu.service;

import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.menu.dto.MenuRequest;
import com.team7.cafe.menu.dto.MenuResponse;
import com.team7.cafe.menu.entity.Menu;
import com.team7.cafe.menu.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;

    // 명시적 생성자 주입 [2026.11.02/안형빈]
    public MenuService(MenuRepository menuRepository, CategoryRepository categoryRepository) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
    }

    // [1] A07: 신규 메뉴 등록 [2026.11.02/안형빈]
    public MenuResponse createMenu(MenuRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. id=" + request.getCategoryId()));

        Menu menu = new Menu(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                category
        );

        Menu savedMenu = menuRepository.save(menu);
        return MenuResponse.from(savedMenu);
    }

    // [2] A08: 메뉴 단건 상세 조회 [2026.11.02/안형빈]
    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다. id=" + id));
        return MenuResponse.from(menu);
    }

    // [3] A06: 전체 메뉴 목록 ID 오름차순 조회 [2026.11.02/안형빈]
    @Transactional(readOnly = true)
    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAllByOrderByIdAsc().stream()
                .map(MenuResponse::from)
                .toList();
    }

    // [4] A15: 카테고리별 메뉴 목록 조회 [2026.11.02/안형빈]
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenusByCategoryId(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. id=" + categoryId);
        }
        return menuRepository.findByCategoryIdOrderByIdAsc(categoryId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    // [5] A09: 메뉴 정보 수정 [2026.11.02/안형빈]
    public MenuResponse updateMenu(Long id, MenuRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다. id=" + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. id=" + request.getCategoryId()));

        menu.update(request.getName(), request.getDescription(), request.getPrice(), category);
        return MenuResponse.from(menu);
    }

    // [6] A10: 메뉴 삭제 [2026.11.02/안형빈]
    public void deleteMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다. id=" + id));
        menuRepository.delete(menu);
    }

    // [7] A21: 메뉴 판매 상태 토글(ON/OFF) [2026.11.02/안형빈]
    public MenuResponse toggleAvailability(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메뉴입니다. id=" + id));

        menu.changeAvailability(!menu.isAvailable());
        return MenuResponse.from(menu);
    }

    // [8] A20: 메뉴명 키워드 검색 [2026.11.02/안형빈]
    // [추가] 쿼리로 검색된 결과를, DTO로 변환하는 단순 읽기이므로, readOnly = true 설정
    @Transactional(readOnly = true)
    public List<MenuResponse> searchMenus(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return menuRepository.search(keyword).stream()
                .map(MenuResponse::from)
                .toList();
    }
}