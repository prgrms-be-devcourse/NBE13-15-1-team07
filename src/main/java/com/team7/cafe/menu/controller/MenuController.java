package com.team7.cafe.menu.controller;

import com.team7.cafe.menu.dto.MenuAvailabilityRequest;
import com.team7.cafe.menu.dto.MenuRequest;
import com.team7.cafe.menu.dto.MenuResponse;
import com.team7.cafe.menu.service.MenuService;
import jakarta.validation.Valid;
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

import java.util.List;

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
    @GetMapping("/menus/{id}")
    public ResponseEntity<MenuResponse> getMenuById(@PathVariable("id") Long id) {
        MenuResponse response = menuService.getMenuById(id);
        return ResponseEntity.ok(response);
    }

    // [3] A08: 신규 메뉴 등록 [2026.11.02/안형빈]
    @PostMapping("/menus")
    public ResponseEntity<MenuResponse> createMenu(@Valid @RequestBody MenuRequest request) {
        MenuResponse response = menuService.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // [4] A09: 메뉴 정보 일반 수정 [2026.11.02/안형빈]
    @PutMapping("/menus/{id}")
    public ResponseEntity<MenuResponse> updateMenu(
            @PathVariable("id") Long id,
            @Valid @RequestBody MenuRequest request
    ) {
        MenuResponse response = menuService.updateMenu(id, request);
        return ResponseEntity.ok(response);
    }

    // [5] A10: 메뉴 삭제 [2026.11.02/안형빈]
    @DeleteMapping("/menus/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable("id") Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    // [6] A15: 카테고리별 메뉴 목록 조회 [2026.11.02/안형빈]
    @GetMapping("/categories/{id}/menus")
    public ResponseEntity<List<MenuResponse>> getMenusByCategoryId(@PathVariable("id") Long id) {
        List<MenuResponse> responses = menuService.getMenusByCategoryId(id);
        return ResponseEntity.ok(responses);
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
        List<MenuResponse> responses = menuService.searchMenus(keyword);
        return ResponseEntity.ok(responses);
    }

    // [9] A21: 판매 여부 변경 (JSON boolean 바디 검증) [2026.11.02/안형빈]
    @PutMapping("/menus/{id}/availability")
    public ResponseEntity<MenuResponse> updateAvailability(
            @PathVariable("id") Long id,
            @Valid @RequestBody MenuAvailabilityRequest request
    ) {
        MenuResponse response = menuService.updateAvailability(id, request.getAvailable());
        return ResponseEntity.ok(response);
    }
}