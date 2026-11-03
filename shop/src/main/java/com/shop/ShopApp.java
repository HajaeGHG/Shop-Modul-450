package com.shop;

import com.shop.model.*;
import com.shop.repository.*;
import com.shop.service.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ShopApp {

    public static void main(String[] args) throws Exception {
        // Verbindung (In-Memory H2, bleibt offen solange JVM läuft)
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:shopdb", "sa", "");

        // Schema anlegen
        CustomerRepository customerRepo = new CustomerRepository(conn);
        ProductRepository productRepo = new ProductRepository(conn);
        OrderRepository orderRepo = new OrderRepository(conn);

        customerRepo.initSchema();
        productRepo.initSchema();
        orderRepo.initSchema();

        // Beispieldaten
        seedProducts(productRepo);

        // Services verdrahten
        PricingService pricingService = new PricingService();
        CustomerService customerService = new CustomerService(customerRepo);
        CartService cartService = new CartService(productRepo);
        OrderService orderService = new OrderService(orderRepo, productRepo, customerRepo, pricingService, customerService);

        // === Demo-Ablauf ===

        // 1. Kunde registrieren
        Customer customer = customerService.register("max@example.com", "Max Muster");
        System.out.println("Kunde: " + customer.name() + " | Tier: " + customer.tier());

        // 2. Warenkorb füllen
        List<CartItem> items = new ArrayList<>();
        Cart cart = new Cart(items);

        cart = cartService.addToCart(cart.items(), "ELEC-001", 2);
        cart = cartService.addToCart(cart.items(), "BOOK-001", 6);
        cart = cartService.addToCart(cart.items(), "FOOD-001", 1);
        System.out.println("Warenkorb: " + cart.totalQuantity() + " Artikel, " + cart.items().size() + " Positionen");

        // 3. Bestellung aufgeben
        Order order = orderService.placeOrder(customer.id(), cart);
        System.out.println("\n--- Bestellung #" + order.id() + " ---");
        System.out.printf("  Zwischensumme:  CHF %s%n", order.subtotal());
        System.out.printf("  Rabatt:         CHF %s%n", order.discount());
        System.out.printf("  Versand:        CHF %s%n", order.shipping());
        System.out.printf("  MwSt:           CHF %s%n", order.tax());
        System.out.printf("  Total:          CHF %s%n", order.total());
        System.out.println("  Status:         " + order.status());

        // 4. Statuswechsel
        order = orderService.confirm(order.id());
        System.out.println("  → " + order.status());

        order = orderService.ship(order.id());
        System.out.println("  → " + order.status());

        order = orderService.deliver(order.id());
        System.out.println("  → " + order.status());

        // 5. Punkte prüfen
        Customer updated = customerRepo.findById(customer.id()).orElseThrow();
        System.out.println("\nTreuepunkte: " + updated.loyaltyPoints() + " | Tier: " + updated.tier());

        // 6. Lagerbestand prüfen
        System.out.println("\n--- Lagerbestand ---");
        for (Product p : productRepo.findByCategory(Product.Category.ELECTRONICS)) {
            System.out.printf("  %s: %d Stk.%n", p.name(), p.stockQuantity());
        }
        for (Product p : productRepo.findByCategory(Product.Category.BOOKS)) {
            System.out.printf("  %s: %d Stk.%n", p.name(), p.stockQuantity());
        }

        conn.close();
    }

    private static void seedProducts(ProductRepository repo) throws Exception {
        repo.save(new Product(null, "ELEC-001", "USB-C Kabel 2m", new BigDecimal("19.90"), 100, Product.Category.ELECTRONICS));
        repo.save(new Product(null, "ELEC-002", "Bluetooth Kopfhörer", new BigDecimal("79.00"), 30, Product.Category.ELECTRONICS));
        repo.save(new Product(null, "ELEC-003", "Webcam HD", new BigDecimal("45.50"), 15, Product.Category.ELECTRONICS));
        repo.save(new Product(null, "BOOK-001", "Clean Code", new BigDecimal("42.00"), 50, Product.Category.BOOKS));
        repo.save(new Product(null, "BOOK-002", "Domain-Driven Design", new BigDecimal("55.00"), 20, Product.Category.BOOKS));
        repo.save(new Product(null, "CLOTH-001", "T-Shirt Bio", new BigDecimal("24.90"), 200, Product.Category.CLOTHING));
        repo.save(new Product(null, "FOOD-001", "Schoggi 200g", new BigDecimal("8.50"), 500, Product.Category.FOOD));
        repo.save(new Product(null, "FOOD-002", "Kaffee Bohnen 1kg", new BigDecimal("18.00"), 80, Product.Category.FOOD));
    }
}