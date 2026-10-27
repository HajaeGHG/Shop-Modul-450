package com.shop.service;

import com.shop.exception.DuplicateCustomerException;
import com.shop.model.Customer;
import com.shop.repository.CustomerRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer register(String email, String name) throws SQLException {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (customerRepository.findByEmail(email.toLowerCase()).isPresent()) {
            throw new DuplicateCustomerException(email);
        }

        Customer customer = new Customer(null, email.toLowerCase(), name.trim(),
            LocalDate.now(), 0, Customer.Tier.BRONZE);
        return customerRepository.save(customer);
    }

    // Treuepunkte: 1 Punkt pro CHF, Bonus an Geburtstag (+50%)
    public Customer earnPoints(Long customerId, BigDecimal amount) throws SQLException {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        int points = amount.intValue();

        // Geburtstagsbonus: +50% wenn heute Geburtstag (Vereinfachung: Tag == Registrierungstag)
        if (LocalDate.now().getDayOfMonth() == customer.registrationDate().getDayOfMonth()
                && LocalDate.now().getMonthValue() == customer.registrationDate().getMonthValue()) {
            points = (int) (points * 1.5);
        }

        int newPoints = customer.loyaltyPoints() + points;
        return customerRepository.updatePoints(customerId, newPoints);
    }

    public Customer redeemPoints(Long customerId, int points) throws SQLException {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        if (points > customer.loyaltyPoints()) {
            throw new IllegalArgumentException("Insufficient loyalty points");
        }

        return customerRepository.updatePoints(customerId, customer.loyaltyPoints() - points);
    }

    // VIP-Erkennung: Kunde ist VIP wenn > 2 Jahre registriert UND GOLD oder höher
    public boolean isVip(Customer customer) {
        long yearsRegistered = ChronoUnit.YEARS.between(customer.registrationDate(), LocalDate.now());
        return yearsRegistered >= 2
            && (customer.tier() == Customer.Tier.GOLD || customer.tier() == Customer.Tier.PLATINUM);
    }
}