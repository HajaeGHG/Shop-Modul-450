package com.shop.service;

import com.shop.exception.InsufficientStockException;
import com.shop.model.Cart;
import com.shop.model.CartItem;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CartService {

    private static final int MAX_ITEMS_PER_ORDER = 50;
    private static final int MAX_QUANTITY_PER_ITEM = 20;

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Cart addToCart(List<CartItem> currentItems, String sku, int quantity) throws SQLException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));

        // Prüfen ob Produkt bereits im Warenkorb
        List<CartItem> items = new ArrayList<>(currentItems);
        Optional<CartItem> existing = items.stream()
                .filter(i -> i.product().sku().equals(sku))
                .findFirst();

        int newQuantity;
        if (existing.isPresent()) {
            newQuantity = existing.get().quantity() + quantity;
            items.remove(existing.get());
        } else {
            newQuantity = quantity;
        }

        if (newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException("Max " + MAX_QUANTITY_PER_ITEM + " per item");
        }

        if (!product.isInStock(newQuantity)) {
            throw new InsufficientStockException(sku, newQuantity, product.stockQuantity());
        }

        items.add(new CartItem(product, newQuantity));

        if (items.stream().mapToInt(CartItem::quantity).sum() > MAX_ITEMS_PER_ORDER) {
            throw new IllegalArgumentException("Max " + MAX_ITEMS_PER_ORDER + " total items");
        }

        return new Cart(items);
    }

    public Cart removeFromCart(List<CartItem> currentItems, String sku) {
        List<CartItem> items = new ArrayList<>(currentItems);
        items.removeIf(i -> i.product().sku().equals(sku));
        return new Cart(items);
    }

    public Cart updateQuantity(List<CartItem> currentItems, String sku, int newQuantity) throws SQLException {
        if (newQuantity <= 0) {
            return removeFromCart(currentItems, sku);
        }
        if (newQuantity > MAX_QUANTITY_PER_ITEM) {
            throw new IllegalArgumentException("Max " + MAX_QUANTITY_PER_ITEM + " per item");
        }

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + sku));

        if (!product.isInStock(newQuantity)) {
            throw new InsufficientStockException(sku, newQuantity, product.stockQuantity());
        }

        List<CartItem> items = new ArrayList<>();
        for (CartItem item : currentItems) {
            if (item.product().sku().equals(sku)) {
                items.add(new CartItem(product, newQuantity));
            } else {
                items.add(item);
            }
        }
        return new Cart(items);
    }

    public BigDecimal getAverageItemPrice(List<CartItem> currentItems) {
        if (currentItems == null || currentItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = currentItems.stream()
            .map(item -> item.product().price())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(currentItems.size()), 2, RoundingMode.HALF_UP);
    }

        public static int getMaxQuantityPerItem()
        {
            return MAX_QUANTITY_PER_ITEM;
        }

        public static int getMaxQuantityPerOrder()
        {
            return MAX_ITEMS_PER_ORDER;
        }
}