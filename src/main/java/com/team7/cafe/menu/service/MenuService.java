package com.team7.cafe.menu.service;

import com.team7.cafe.category.CategoryErrorCode;
import com.team7.cafe.category.entity.Category;
import com.team7.cafe.category.repository.CategoryRepository;
import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.menu.MenuErrorCode;
import com.team7.cafe.menu.dto.MenuPageResponse;
import com.team7.cafe.menu.dto.MenuRequest;
import com.team7.cafe.menu.dto.MenuResponse;
import com.team7.cafe.menu.entity.Menu;
import com.team7.cafe.menu.repository.MenuRepository;
import com.team7.cafe.order.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

// 입력 검증(400)은 컨트롤러에서 끝난 값만 들어온다. 여기서는 404 → 409 순서로 판단한다 [2026.11.03/안형빈]
// 저장·수정·삭제는 모든 검사를 통과한 뒤 마지막에 실행한다 [2026.11.03/안형빈]
// 클래스 전체를 읽기 전용 트랜잭션으로 두고, 데이터를 바꾸는 메서드(등록·수정·삭제·판매 상태 변경)에만 @Transactional을 열어 준다 [2026.11.06/안형빈]
// 심화 3 Redis 캐시: getAllMenus(A06)에 @Cacheable을 붙여 결과를 "menus::all" 키로 캐시한다 [2026.11.06/김지호]
// createMenu, updateMenu, deleteMenu, updateAvailability에 @CacheEvict를 붙여 메뉴가 바뀌면 캐시를 비운다 [2026.11.06/김지호]
// 캐시 TTL(30초)과 Redis 연결 설정은 global/config/CacheConfig에 있다 [2026.11.06/김지호]
// getAllMenus는 Redis 역직렬화를 위해 toList() 대신 Collectors.toList()(ArrayList)로 반환한다 [2026.11.06/김지호]
@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public MenuService(MenuRepository menuRepository, CategoryRepository categoryRepository, OrderItemRepository orderItemRepository) {
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // [1] A08: 신규 메뉴 등록 [2026.11.02/안형빈]
    @Transactional
    @CacheEvict(cacheNames = "menus", allEntries = true)
    public MenuResponse createMenu(MenuRequest request) {
        Category category = getCategory(request.getCategoryId());

        Menu menu = new Menu(
                category,
                request.getName(),
                request.getPrice(),
                request.getDescription()
        );

        Menu savedMenu = menuRepository.save(menu);
        return MenuResponse.from(savedMenu);
    }

    // [2] A07: 메뉴 단건 상세 조회 [2026.11.02/안형빈]
    public MenuResponse getMenuById(Long id) {
        return MenuResponse.from(getEntity(id));
    }

    // [3] A06: 전체 메뉴 목록 ID 오름차순 조회 [2026.11.02/안형빈]
    @Cacheable(cacheNames = "menus", key = "'all'")
    public List<MenuResponse> getAllMenus() {
        return menuRepository.findAllByOrderByIdAsc().stream()
                .map(MenuResponse::from)
                // Redis 캐시에서 꺼낼 때 목록 타입 정보가 필요해 toList() 대신 ArrayList로 모은다 [2026.11.06/김지호]
                .collect(Collectors.toList());
    }

    // [4] A15: 카테고리별 메뉴 목록 조회 [2026.11.02/안형빈]
    public List<MenuResponse> getMenusByCategoryId(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ApiException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        return menuRepository.findByCategoryIdOrderByIdAsc(categoryId).stream()
                .map(MenuResponse::from)
                .toList();
    }

    // [5] A09: 메뉴 정보 수정 (메뉴 404 → 카테고리 404 순서) [2026.11.02/안형빈]
    @Transactional
    @CacheEvict(cacheNames = "menus", allEntries = true)
    public MenuResponse updateMenu(Long id, MenuRequest request) {
        Menu menu = getEntity(id);
        Category category = getCategory(request.getCategoryId());

        menu.update(category, request.getName(), request.getPrice(), request.getDescription());
        return MenuResponse.from(menu);
    }

    // [6] A10: 메뉴 삭제 [2026.11.03/안형빈]
    @Transactional
    @CacheEvict(cacheNames = "menus", allEntries = true)
    public void deleteMenu(Long id) {
        Menu menu = getEntity(id);

        // 한 번이라도 주문된 메뉴(취소 포함)는 409 MENU_IN_USE [2026.11.03/안형빈]
        if (orderItemRepository.existsByMenuId(id)) {
            throw new ApiException(MenuErrorCode.MENU_IN_USE);
        }

        menuRepository.delete(menu);
    }

    // [7] A19: 판매 중(available=true) 메뉴 목록 조회 추가 [2026.11.02/안형빈]
    public List<MenuResponse> getAvailableMenus() {
        return menuRepository.findByAvailableTrueOrderByIdAsc().stream()
                .map(MenuResponse::from)
                .toList();
    }

    // [8] A20: 메뉴명 키워드 검색 (keyword는 컨트롤러에서 trim·검증 완료) [2026.11.02/안형빈]
    public List<MenuResponse> searchMenus(String keyword) {
        return menuRepository.search(keyword).stream()
                .map(MenuResponse::from)
                .toList();
    }

    // [9] A21: 메뉴 판매 상태 변경 [2026.11.02/안형빈]
    @Transactional
    @CacheEvict(cacheNames = "menus", allEntries = true)
    public MenuResponse updateAvailability(Long id, boolean available) {
        Menu menu = getEntity(id);

        menu.changeAvailability(available);
        return MenuResponse.from(menu);
    }

    // [14] 심화: 메뉴 페이징 조회 (GET /menus/page) [2026.11.06/안형빈]
    public MenuPageResponse getMenusPage(int page, int size){
        // 1. Pageable 객체 생성 (id 기준 오름차순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));

        // 2. Repository 조회 (Page<Menu> 반환)
        Page<Menu> menuPage = menuRepository.findAll(pageable);

        // 3. 엔티티 Page를 DTO Page로 변환 (Page<MenuResponse>)
        Page<MenuResponse> menuResponsePage = menuPage.map(MenuResponse::from);

        // 4. 최종 MenuPageResponse로 감싸서 반환
        return MenuPageResponse.from(menuResponsePage);
    }

    // ---- 다른 도메인용 (카테고리: 김아리, 주문: 김지호) [2026.11.03/안형빈] ----

    // [10] 타 도메인용: ID로 메뉴 엔티티 조회, 없으면 404 MENU_NOT_FOUND [2026.11.03/안형빈]
    public Menu getEntity(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new ApiException(MenuErrorCode.MENU_NOT_FOUND));
    }

    // [11] 타 도메인용: 카테고리에 메뉴가 하나라도 있는지 (A05 CATEGORY_IN_USE 판단용) [2026.11.03/안형빈]
    public boolean existsByCategory(Long categoryId) {
        return menuRepository.existsByCategoryId(categoryId);
    }

    // [12] 타 도메인용: 주문 가능한 메뉴 조회 (A11, A23), 없으면 404 / 판매 중지면 409 MENU_UNAVAILABLE [2026.11.03/안형빈]
    public Menu getOrderableMenu(Long id) {
        Menu menu = getEntity(id);
        if (!menu.isAvailable()) {
            throw new ApiException(MenuErrorCode.MENU_UNAVAILABLE);
        }
        return menu;
    }

    // [13] 카테고리 조회 공통 : 없으면 404 CATEGORY_NOT_FOUND [2026.11.03/안형빈]
    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }
}
