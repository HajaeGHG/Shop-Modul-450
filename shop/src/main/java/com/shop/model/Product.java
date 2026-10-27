package com.shop.model;

import java.math.BigDecimal;

public record Product(
    Long id,
    String sku,
    String name,
    BigDecimal price,
    int stockQuantity,
    Category category
) {
    public enum Category { ELECTRONICS, CLOTHING, FOOD, BOOKS }

    public boolean isInStock(int quantity) {
        return stockQuantity >= quantity;
    }

    public Product withStock(int newStock) {
        return new Product(id, sku, name, price, newStock, category);
    }

    public Product withId(Long id) {
        return new Product(id, sku, name, price, stockQuantity, category);
    }
}