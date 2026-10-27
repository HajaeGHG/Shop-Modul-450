package com.shop.repository;

import com.shop.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class OrderRepository {

    private final Connection connection;

    public OrderRepository(Connection connection) {
        this.connection = connection;
    }

    public void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    customer_id BIGINT NOT NULL,
                    subtotal DECIMAL(10,2),
                    discount DECIMAL(10,2),
                    shipping DECIMAL(10,2),
                    tax DECIMAL(10,2),
                    total DECIMAL(10,2),
                    status VARCHAR(20),
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS order_items (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    unit_price DECIMAL(10,2) NOT NULL
                )
            """);
        }
    }

    public Order save(Order order, List<CartItem> items) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String sql = "INSERT INTO orders (customer_id, subtotal, discount, shipping, tax, total, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            long orderId;
            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, order.customerId());
                ps.setBigDecimal(2, order.subtotal());
                ps.setBigDecimal(3, order.discount());
                ps.setBigDecimal(4, order.shipping());
                ps.setBigDecimal(5, order.tax());
                ps.setBigDecimal(6, order.total());
                ps.setString(7, order.status().name());
                ps.setTimestamp(8, Timestamp.valueOf(order.createdAt()));
                ps.setTimestamp(9, Timestamp.valueOf(order.updatedAt()));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    orderId = rs.getLong(1);
                }
            }
            for (CartItem item : items) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO order_items (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)")) {
                    ps.setLong(1, orderId);
                    ps.setLong(2, item.product().id());
                    ps.setInt(3, item.quantity());
                    ps.setBigDecimal(4, item.product().price());
                    ps.executeUpdate();
                }
            }
            connection.commit();
            return order.withId(orderId);
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public Optional<Order> findById(Long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM orders WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    List<CartItem> items = loadItems(id);
                    return Optional.of(mapOrder(rs, items));
                }
            }
        }
        return Optional.empty();
    }

    public List<Order> findByCustomerId(Long customerId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM orders WHERE customer_id = ?")) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapOrder(rs, loadItems(rs.getLong("id"))));
                }
            }
        }
        return orders;
    }

    public void updateStatus(Long id, OrderStatus status, LocalDateTime updatedAt) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.valueOf(updatedAt));
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    private List<CartItem> loadItems(Long orderId) throws SQLException {
        List<CartItem> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT oi.product_id, oi.quantity, p.sku, p.name, p.price, p.stock_quantity, p.category FROM order_items oi JOIN products p ON p.id = oi.product_id WHERE oi.order_id = ?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                        rs.getLong("product_id"), rs.getString("sku"), rs.getString("name"),
                        rs.getBigDecimal("price"), rs.getInt("stock_quantity"),
                        Product.Category.valueOf(rs.getString("category"))
                    );
                    items.add(new CartItem(product, rs.getInt("quantity")));
                }
            }
        }
        return items;
    }

    private Order mapOrder(ResultSet rs, List<CartItem> items) throws SQLException {
        return new Order(
            rs.getLong("id"), rs.getLong("customer_id"), items,
            rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
            rs.getBigDecimal("shipping"), rs.getBigDecimal("tax"),
            rs.getBigDecimal("total"), OrderStatus.valueOf(rs.getString("status")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}