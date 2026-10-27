package com.shop.repository;

import com.shop.model.Customer;
import java.sql.*;
import java.util.Optional;

public class CustomerRepository {

    private final Connection connection;

    public CustomerRepository(Connection connection) {
        this.connection = connection;
    }

    public void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    registration_date DATE NOT NULL,
                    loyalty_points INT DEFAULT 0
                )
            """);
        }
    }

    public Optional<Customer> findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, email, name, registration_date, loyalty_points FROM customers WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Customer> findById(Long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, email, name, registration_date, loyalty_points FROM customers WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Customer save(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (email, name, registration_date, loyalty_points) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.email());
            ps.setString(2, customer.name());
            ps.setDate(3, Date.valueOf(customer.registrationDate()));
            ps.setInt(4, customer.loyaltyPoints());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return customer.withId(rs.getLong(1));
            }
        }
    }

    public Customer updatePoints(Long id, int points) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE customers SET loyalty_points = ? WHERE id = ?")) {
            ps.setInt(1, points);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        return findById(id).orElseThrow();
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        int points = rs.getInt("loyalty_points");
        return new Customer(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("name"),
            rs.getDate("registration_date").toLocalDate(),
            points,
            Customer.tierFromPoints(points)
        );
    }
}