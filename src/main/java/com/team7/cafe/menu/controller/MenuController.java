package com.team7.cafe.menu.controller;

import com.team7.cafe.menu.dto.*;
import com.team7.cafe.menu.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 입력 검증(400)은 컨트롤러에서 끝내고, 서비스는 검증된 값만 받아 404 → 409를 판단한다 [2026.11.03/안형빈]
// 경로 ID는 String으로 받아 직접 파싱해야 fieldErrors 키(menuId, categoryId)를 통제할 수 있다 [2026.11.03/안형빈]
@RestController
@RequestMapping("/api/v1")
public class MenuController {

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // [1] A06: 전체 메뉴 목록 조회 [2026.11.02/안형빈]
    @GetMapping("/menus")
    public ResponseEntity<List<MenuResponse>> getAllMenus() {
        List<MenuResponse> responses = menuService.getAllMenus();
        return ResponseEntity.ok(responses);
    }

    // [2] A07: 메뉴 상세 단건 조회 [2026.11.02/안형빈]
    @GetMapping("/menus/{menuId}")
    public ResponseEntity<MenuResponse> getMenuById(@PathVariable("menuId") String menuId) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long id = MenuValidator.id(menuId, "menuId", errors);
        MenuValidator.throwIfAny(errors);

        return ResponseEntity.ok(menuService.getMenuById(id));
    }

    // [3] A08: 신규 메뉴 등록 [2026.11.02/안형빈]
    @PostMapping("/menus")
    public ResponseEntity<MenuResponse> createMenu(@RequestBody Map<String, Object> body) {
        Map<String, String> errors = new LinkedHashMap<>();
        MenuRequest request = MenuRequest.parse(body, errors);
        MenuValidator.throwIfAny(errors);

        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.createMenu(request));
    }

    // [4] A09: 메뉴 정보 일반 수정 [2026.11.02/안형빈]
    @PutMapping("/menus/{menuId}")
    public ResponseEntity<MenuResponse> updateMenu(
            @PathVariable("menuId") String menuId,
            @RequestBody Map<String, Object> body
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long id = MenuValidator.id(menuId, "menuId", errors);
        MenuRequest request = MenuRequest.parse(body, errors);
        MenuValidator.throwIfAny(errors);

        return ResponseEntity.ok(menuService.updateMenu(id, request));
    }

    // [5] A10: 메뉴 삭제 [2026.11.02/안형빈]
    @DeleteMapping("/menus/{menuId}")
    public ResponseEntity<Void> deleteMenu(@PathVariable("menuId") String menuId) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long id = MenuValidator.id(menuId, "menuId", errors);
        MenuValidator.throwIfAny(errors);

        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    // [6] A15: 카테고리별 메뉴 목록 조회 [2026.11.02/안형빈]
    @GetMapping("/categories/{categoryId}/menus")
    public ResponseEntity<List<MenuResponse>> getMenusByCategoryId(@PathVariable("categoryId") String categoryId) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long id = MenuValidator.id(categoryId, "categoryId", errors);
        MenuValidator.throwIfAny(errors);

        return ResponseEntity.ok(menuService.getMenusByCategoryId(id));
    }

    // [7] A19: 판매 중 메뉴 목록 조회 [2026.11.02/안형빈]
    @GetMapping("/menus/available")
    public ResponseEntity<List<MenuResponse>> getAvailableMenus() {
        List<MenuResponse> responses = menuService.getAvailableMenus();
        return ResponseEntity.ok(responses);
    }

    // [8] A20: 메뉴명 키워드 검색 [2026.11.02/안형빈]
    @GetMapping("/menus/search")
    public ResponseEntity<List<MenuResponse>> searchMenus(@RequestParam(name = "keyword", required = false) String keyword) {
        Map<String, String> errors = new LinkedHashMap<>();
        String trimmed = MenuValidator.keyword(keyword, errors);
        MenuValidator.throwIfAny(errors);

        return ResponseEntity.ok(menuService.searchMenus(trimmed));
    }

    // [9] A21: 판매 여부 변경 (JSON boolean 바디 검증) [2026.11.02/안형빈]
    @PutMapping("/menus/{menuId}/availability")
    public ResponseEntity<MenuResponse> updateAvailability(
            @PathVariable("menuId") String menuId,
            @RequestBody Map<String, Object> body
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        Long id = MenuValidator.id(menuId, "menuId", errors);
        MenuAvailabilityRequest request = MenuAvailabilityRequest.parse(body, errors);
        MenuValidator.throwIfAny(errors);

        return ResponseEntity.ok(menuService.updateAvailability(id, request.getAvailable()));
    }

    // [10] 심화: 메뉴 페이징 조회 (GET /api/v1/menus/page?page=&size=) [2026.11.06/안형빈]
    @GetMapping("/menus/page")
    public ResponseEntity<MenuPageResponse> getMenusPage (
            @RequestParam(name = "page", required = false) String pageStr,
            @RequestParam(name = "size", required = false) String sizeStr
    ){

        Map<String, String> errors = new LinkedHashMap<>();
        int page =  MenuValidator.page(pageStr,errors);
        int size = MenuValidator.size(sizeStr,errors);

        MenuValidator.throwIfAny(errors);

        return ResponseEntity.ok(menuService.getMenusPage(page, size));
    }
}
