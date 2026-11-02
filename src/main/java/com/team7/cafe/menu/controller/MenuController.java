package com.team7.cafe.menu.controller;

import com.team7.cafe.menu.dto.MenuRequest;
import com.team7.cafe.menu.dto.MenuResponse;
import com.team7.cafe.menu.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    @Autowired
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // [1] A07: 신규 메뉴 등록 [2026.11.02/안형빈]
    @PostMapping
    public ResponseEntity<MenuResponse> createMenu(@Valid @RequestBody MenuRequest request) {
        MenuResponse response = menuService.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // [2] A08: 메뉴 단건 상세 조회 [2026.11.02/안형빈]
    @GetMapping("/{id}")
    public ResponseEntity<MenuResponse> getMenuById(@PathVariable("id") Long id) {
        MenuResponse response = menuService.getMenuById(id);
        return ResponseEntity.ok(response);
    }

    // [3] A06: 전체 메뉴 목록 조회 [2026.11.02/안형빈]
    @GetMapping
    public ResponseEntity<List<MenuResponse>> getAllMenus() {
        List<MenuResponse> responses = menuService.getAllMenus();
        return ResponseEntity.ok(responses);
    }

    // [4] A15: 특정 카테고리 소속 메뉴 목록 조회 [2026.11.02/안형빈]
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuResponse>> getMenusByCategoryId(@PathVariable("categoryId") Long categoryId) {
        List<MenuResponse> responses = menuService.getMenusByCategoryId(categoryId);
        return ResponseEntity.ok(responses);
    }

    // [5] A09: 메뉴 정보 수정 [2026.11.02/안형빈]
    @PutMapping("/{id}")
    public ResponseEntity<MenuResponse> updateMenu(
            @PathVariable("id") Long id,
            @Valid @RequestBody MenuRequest request
    ) {
        MenuResponse response = menuService.updateMenu(id, request);
        return ResponseEntity.ok(response);
    }

    // [6] A10: 메뉴 삭제 [2026.11.02/안형빈]
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable("id") Long id) {
        menuService.deleteMenu(id);
        return ResponseEntity.noContent().build();
    }

    // [7] A21: 메뉴 판매 상태 토글 (ON/OFF) [2026.11.02/안형빈]
    @PatchMapping("/{id}/status")
    public ResponseEntity<MenuResponse> toggleAvailability(@PathVariable("id") Long id) {
        MenuResponse response = menuService.toggleAvailability(id);
        return ResponseEntity.ok(response);
    }

    // [8] A20: 메뉴명 키워드 검색 [2026.11.02/안형빈]
    @GetMapping("/search")
    public ResponseEntity<List<MenuResponse>> searchMenus(@RequestParam("keyword") String keyword) {
        List<MenuResponse> responses = menuService.searchMenus(keyword);
        return ResponseEntity.ok(responses);
    }
}