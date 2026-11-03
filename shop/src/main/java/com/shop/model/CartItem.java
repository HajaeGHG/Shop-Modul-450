package com.shop.model;

public record CartItem(Product product, int quantity) {
    public CartItem withQuantity(int qty) {
        return new CartItem(product, qty);
    }

    public int getQuantity() {
        return quantity;
    }
}