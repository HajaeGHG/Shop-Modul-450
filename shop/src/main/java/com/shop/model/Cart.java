package com.shop.model;

import java.util.List;

public record Cart(List<CartItem> items) {
    public int totalQuantity() {
        return items.stream().mapToInt(CartItem::quantity).sum();
    }
}