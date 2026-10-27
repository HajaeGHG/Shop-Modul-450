package com.shop.service;

import com.shop.model.Cart;
import com.shop.model.CartItem;
import com.shop.model.Customer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PricingService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal FLAT_SHIPPING = new BigDecimal("5.90");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.081"); // 8.1% CH-MwSt

    // Mengenrabatt: ab 5 Stk. 5%, ab 10 Stk. 10%, ab 20 Stk. 15%
    public BigDecimal bulkDiscount(int quantity) {
        if (quantity >= 20) return new BigDecimal("0.15");
        if (quantity >= 10) return new BigDecimal("0.10");
        if (quantity >= 5) return new BigDecimal("0.05");
        return BigDecimal.ZERO;
    }

    // Treuerabatt nach Tier
    public BigDecimal loyaltyDiscount(Customer.Tier tier) {
        return switch (tier) {
            case BRONZE -> BigDecimal.ZERO;
            case SILVER -> new BigDecimal("0.03");
            case GOLD -> new BigDecimal("0.07");
            case PLATINUM -> new BigDecimal("0.12");
        };
    }

    // Kategorie-spezifische Rabatte
    public BigDecimal categoryDiscount(Customer.Tier tier, com.shop.model.Product.Category category) {
        // GOLD und PLATINUM bekommen 10% auf Bücher
        if ((tier == Customer.Tier.GOLD || tier == Customer.Tier.PLATINUM)
                && category == com.shop.model.Product.Category.BOOKS) {
            return new BigDecimal("0.10");
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateSubtotal(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart.items()) {
            BigDecimal lineTotal = item.product().price()
                .multiply(BigDecimal.valueOf(item.quantity()));
            BigDecimal discount = bulkDiscount(item.quantity());
            BigDecimal discounted = lineTotal.multiply(BigDecimal.ONE.subtract(discount));
            subtotal = subtotal.add(discounted);
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateLoyaltyDiscount(BigDecimal subtotal, Customer customer) {
        BigDecimal rate = loyaltyDiscount(customer.tier());
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCategoryDiscount(Cart cart, Customer customer) {
        BigDecimal discount = BigDecimal.ZERO;
        for (CartItem item : cart.items()) {
            BigDecimal rate = categoryDiscount(customer.tier(), item.product().category());
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal lineTotal = item.product().price()
                    .multiply(BigDecimal.valueOf(item.quantity()));
                discount = discount.add(lineTotal.multiply(rate));
            }
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateShipping(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
            ? BigDecimal.ZERO
            : FLAT_SHIPPING;
    }

    public BigDecimal calculateTax(BigDecimal taxableAmount) {
        return taxableAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotal(BigDecimal subtotal, BigDecimal loyaltyDisc,
                                     BigDecimal categoryDisc, BigDecimal shipping) {
        BigDecimal taxable = subtotal.subtract(loyaltyDisc).subtract(categoryDisc).add(shipping);
        BigDecimal tax = calculateTax(taxable);
        return taxable.add(tax).setScale(2, RoundingMode.HALF_UP);
    }
}