package com.team7.cafe.order.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "line_amount", nullable = false)
    private long lineAmount;

    protected OrderItem() {
    }

    public OrderItem(Long menuId, String menuName, int unitPrice, int quantity) {
        this.menuId = menuId;
        this.menuName = menuName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineAmount = (long) unitPrice * quantity;
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public Long getMenuId() {
        return menuId;
    }

    public String getMenuName() {
        return menuName;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getLineAmount() {
        return lineAmount;
    }
}
