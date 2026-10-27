package com.shop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Order(
    Long id,
    Long customerId,
    List<CartItem> items,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal shipping,
    BigDecimal tax,
    BigDecimal total,
    OrderStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public Order withStatus(OrderStatus newStatus, LocalDateTime now) {
        return new Order(id, customerId, items, subtotal, discount, shipping, tax, total, newStatus, createdAt, now);
    }

    public Order withId(Long id) {
        return new Order(id, customerId, items, subtotal, discount, shipping, tax, total, status, createdAt, updatedAt);
    }
}