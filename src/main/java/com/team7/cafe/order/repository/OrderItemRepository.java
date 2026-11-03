package com.team7.cafe.order.repository;

import com.team7.cafe.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByMenuId(Long menuId);
}
