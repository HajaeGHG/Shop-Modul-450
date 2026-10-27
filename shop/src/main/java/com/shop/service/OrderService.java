package com.shop.service;

import com.shop.exception.InsufficientStockException;
import com.shop.exception.InvalidOrderStateException;
import com.shop.model.*;
import com.shop.repository.CustomerRepository;
import com.shop.repository.OrderRepository;
import com.shop.repository.ProductRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OrderService {

    private static final int CANCELLATION_WINDOW_HOURS = 24;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PricingService pricingService;
    private final CustomerService customerService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository,
                        PricingService pricingService,
                        CustomerService customerService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.pricingService = pricingService;
        this.customerService = customerService;
    }

    public Order placeOrder(Long customerId, Cart cart) throws SQLException {
        if (cart.items().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Lagerbestand prüfen und reservieren
        for (CartItem item : cart.items()) {
            Product product = productRepository.findById(item.product().id())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            if (!product.isInStock(item.quantity())) {
                throw new InsufficientStockException(product.sku(), item.quantity(), product.stockQuantity());
            }
            productRepository.updateStock(product.id(), product.stockQuantity() - item.quantity());
        }

        // Preise berechnen
        BigDecimal subtotal = pricingService.calculateSubtotal(cart);
        BigDecimal loyaltyDisc = pricingService.calculateLoyaltyDiscount(subtotal, customer);
        BigDecimal categoryDisc = pricingService.calculateCategoryDiscount(cart, customer);
        BigDecimal shipping = pricingService.calculateShipping(subtotal);
        BigDecimal total = pricingService.calculateTotal(subtotal, loyaltyDisc, categoryDisc, shipping);

        LocalDateTime now = LocalDateTime.now();
        Order order = new Order(null, customerId, cart.items(), subtotal, loyaltyDisc,
            shipping, pricingService.calculateTax(subtotal.subtract(loyaltyDisc).subtract(categoryDisc).add(shipping)),
            total, OrderStatus.PENDING, now, now);

        Order saved = orderRepository.save(order, cart.items());

        // Treuepunkte gutschreiben
        customerService.earnPoints(customerId, total);

        return saved;
    }

    public Order confirm(Long orderId) throws SQLException {
        Order order = getOrderOrThrow(orderId);
        if (!order.status().canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new InvalidOrderStateException(orderId, order.status().name(), "CONFIRMED");
        }
        LocalDateTime now = LocalDateTime.now();
        orderRepository.updateStatus(orderId, OrderStatus.CONFIRMED, now);
        return order.withStatus(OrderStatus.CONFIRMED, now);
    }

    public Order ship(Long orderId) throws SQLException {
        Order order = getOrderOrThrow(orderId);
        if (!order.status().canTransitionTo(OrderStatus.SHIPPED)) {
            throw new InvalidOrderStateException(orderId, order.status().name(), "SHIPPED");
        }
        LocalDateTime now = LocalDateTime.now();
        orderRepository.updateStatus(orderId, OrderStatus.SHIPPED, now);
        return order.withStatus(OrderStatus.SHIPPED, now);
    }

    public Order deliver(Long orderId) throws SQLException {
        Order order = getOrderOrThrow(orderId);
        if (!order.status().canTransitionTo(OrderStatus.DELIVERED)) {
            throw new InvalidOrderStateException(orderId, order.status().name(), "DELIVERED");
        }
        LocalDateTime now = LocalDateTime.now();
        orderRepository.updateStatus(orderId, OrderStatus.DELIVERED, now);
        return order.withStatus(OrderStatus.DELIVERED, now);
    }

    public Order cancel(Long orderId) throws SQLException {
        Order order = getOrderOrThrow(orderId);

        if (!order.status().isCancellable()) {
            throw new InvalidOrderStateException(orderId, order.status().name(), "CANCELLED");
        }

        // Stornierungsfenster: nur innerhalb von 24h nach Erstellung
        if (order.status() == OrderStatus.CONFIRMED) {
            long hoursSinceCreation = java.time.Duration.between(order.createdAt(), LocalDateTime.now()).toHours();
            if (hoursSinceCreation > CANCELLATION_WINDOW_HOURS) {
                throw new InvalidOrderStateException(orderId, order.status().name(),
                    "CANCELLED (window expired)");
            }
        }

        // Lagerbestand zurückerstatten
        for (CartItem item : order.items()) {
            Product product = productRepository.findById(item.product().id())
                .orElseThrow();
            productRepository.updateStock(product.id(), product.stockQuantity() + item.quantity());
        }

        LocalDateTime now = LocalDateTime.now();
        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED, now);
        return order.withStatus(OrderStatus.CANCELLED, now);
    }

    public Optional<Order> findById(Long id) throws SQLException {
        return orderRepository.findById(id);
    }

    public List<Order> findByCustomer(Long customerId) throws SQLException {
        return orderRepository.findByCustomerId(customerId);
    }

    private Order getOrderOrThrow(Long orderId) throws SQLException {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
}