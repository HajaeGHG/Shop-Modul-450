package com.shop.repository;

import com.shop.model.Product;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepository {

    private final Connection connection;

    public ProductRepository(Connection connection) {
        this.connection = connection;
    }

    public void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    sku VARCHAR(50) NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    stock_quantity INT NOT NULL,
                    category VARCHAR(50) NOT NULL
                )
            """);
        }
    }

    public Optional<Product> findBySku(String sku) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM products WHERE sku = ?")) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<Product> findById(Long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM products WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<Product> findByCategory(Product.Category category) throws SQLException {
        List<Product> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM products WHERE category = ?")) {
            ps.setString(1, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    public Product save(Product product) throws SQLException {
        String sql = "INSERT INTO products (sku, name, price, stock_quantity, category) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, product.sku());
            ps.setString(2, product.name());
            ps.setBigDecimal(3, product.price());
            ps.setInt(4, product.stockQuantity());
            ps.setString(5, product.category().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return product.withId(rs.getLong(1));
            }
        }
    }

    public void updateStock(Long id, int newQuantity) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE products SET stock_quantity = ? WHERE id = ?")) {
            ps.setInt(1, newQuantity);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
            rs.getLong("id"),
            rs.getString("sku"),
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getInt("stock_quantity"),
            Product.Category.valueOf(rs.getString("category"))
        );
    }
}