package com.shop.model;

import java.time.LocalDate;

public record Customer(
    Long id,
    String email,
    String name,
    LocalDate registrationDate,
    int loyaltyPoints,
    Tier tier
) {
    public enum Tier { BRONZE, SILVER, GOLD, PLATINUM }

    public static Tier tierFromPoints(int points) {
        if (points >= 5000) return Tier.PLATINUM;
        if (points >= 2000) return Tier.GOLD;
        if (points >= 500) return Tier.SILVER;
        return Tier.BRONZE;
    }

    public Customer withPoints(int newPoints) {
        return new Customer(id, email, name, registrationDate, newPoints, tierFromPoints(newPoints));
    }

    public Customer withId(Long id) {
        return new Customer(id, email, name, registrationDate, loyaltyPoints, tier);
    }
}