package com.team7.cafe.menu.entity;

import com.team7.cafe.category.entity.Category;
import jakarta.persistence.*;

@Entity
@Table(name = "menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private int price; // 스키마(INT) 및 요구사항에 맞춰 int로 변경

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "available", nullable = false)
    private boolean available;

    protected Menu() {
    }

    // 신규 메뉴 등록용 생성자 (id와 available은 파라미터에서 제외)
    public Menu(Category category, String name, int price, String description) {
        this.category = category;
        this.name = name;
        this.price = price;
        this.description = (description == null) ? "" : description; // null 금지 방어 로직
        this.available = true; // 신규는 항상 true
    }

    // [1] 편의용 메서드 : 단순 내용 수정용 (A09: available은 건드리지 않음)
    public void update(Category category, String name, int price, String description) {
        this.category = category;
        this.name = name;
        this.price = price;
        this.description = (description == null) ? "" : description;
    }

    // [2] 편의용 메서드 : 판매 여부만 변경용 (A21)
    public void changeAvailability(boolean available) {
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAvailable() {
        return available;
    }
}