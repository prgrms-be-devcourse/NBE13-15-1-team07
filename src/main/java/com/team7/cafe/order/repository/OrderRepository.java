package com.team7.cafe.order.repository;

import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") Long id);

    List<Order> findByEmailOrderByCreatedAtDescIdDesc(String email);

    Optional<Order> findFirstByEmailAndShippingDateAndStatus(String email, LocalDate shippingDate, OrderStatus status);
}