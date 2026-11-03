package com.team7.cafe.order.service;

import com.team7.cafe.global.error.ApiException;
import com.team7.cafe.global.error.CommonErrorCode;
import com.team7.cafe.menu.MenuErrorCode;
import com.team7.cafe.menu.entity.Menu;
import com.team7.cafe.menu.service.MenuService;
import com.team7.cafe.order.OrderErrorCode;
import com.team7.cafe.order.dto.OrderCreateRequest;
import com.team7.cafe.order.dto.OrderItemRequest;
import com.team7.cafe.order.dto.OrderResponse;
import com.team7.cafe.order.entity.Order;
import com.team7.cafe.order.entity.OrderItem;
import com.team7.cafe.order.entity.OrderStatus;
import com.team7.cafe.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuService menuService;
    private final ShippingPolicy shippingPolicy;

    @Autowired
    public OrderService(OrderRepository orderRepository, MenuService menuService, ShippingPolicy shippingPolicy) {
        this.orderRepository = orderRepository;
        this.menuService = menuService;
        this.shippingPolicy = shippingPolicy;
    }

    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        validateNoDuplicateMenu(request.items());
        Order order = placeOrder(request.email(), request.address(), request.postcode(), request.items());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse reorder(Long orderId) {
        Order original = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> new ApiException(OrderErrorCode.ORDER_NOT_FOUND));

        List<OrderItemRequest> lines = original.getItems().stream()
                .map(item -> new OrderItemRequest(item.getMenuId(), item.getQuantity()))
                .toList();

        Order order = placeOrder(original.getEmail(), original.getAddress(), original.getPostcode(), lines);
        return OrderResponse.from(order);
    }

    private Order placeOrder(String email, String address, String postcode, List<OrderItemRequest> lines) {
        List<Menu> menus = new ArrayList<>();
        for (OrderItemRequest line : lines) {
            menus.add(menuService.getEntity(line.menuId()));
        }

        for (Menu menu : menus) {
            if(!menu.isAvailable()){
                throw new ApiException(MenuErrorCode.MENU_UNAVAILABLE);
            }
        }

        LocalDateTime now = shippingPolicy.now();
        LocalDate shippingDate = shippingPolicy.shippingDateOf(now);

        Optional<Order> sameBatch = orderRepository.findFirstByEmailAndShippingDateAndStatus(
                email, shippingDate, OrderStatus.PLACED);
        if (sameBatch.isPresent()) {
            Order existing = sameBatch.get();
            if (!existing.getAddress().equals(address) || !existing.getPostcode().equals(postcode)) {
                throw new ApiException(OrderErrorCode.ADDRESS_MISMATCH);
            }
        }

        Order order = Order.place(email, address, postcode, now, shippingDate);
        for (int i = 0; i < lines.size(); i++) {
            Menu menu = menus.get(i);
            order.addItem(new OrderItem(menu.getId(), menu.getName(), menu.getPrice(), lines.get(i).quantity()));
        }

        return orderRepository.save(order);
    }

    private void validateNoDuplicateMenu(List<OrderItemRequest> items) {
        Set<Long> menuIds = new HashSet<>();
        for (OrderItemRequest item : items) {
            if (!menuIds.add(item.menuId())) {
                throw new ApiException(CommonErrorCode.VALIDATION_ERROR,
                        Map.of("items", "같은 메뉴를 중복해서 담을 수 없습니다."));
            }
        }
    }
}
