package com.team7.cafe.order.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "postcode", nullable = false, length = 5)
    private String postcode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "shipping_date", nullable = false)
    private LocalDate shippingDate;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "memo", nullable = false, length = 200)
    private String memo;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public static Order place(String email, String address, String postcode, LocalDateTime createdAt, LocalDate shippingDate) {
        Order order = new Order();
        order.email = email;
        order.address = address;
        order.postcode = postcode;
        order.createdAt = createdAt;
        order.shippingDate = shippingDate;
        order.status = OrderStatus.PLACED;
        order.memo = "";
        return order;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.assignOrder(this);
        this.totalAmount += item.getLineAmount();
    }

    public void cancel(LocalDateTime cancelledAt) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
    }

    public String getPostcode() {
        return postcode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDate getShippingDate() {
        return shippingDate;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getMemo() {
        return memo;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
