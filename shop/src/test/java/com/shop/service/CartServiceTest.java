package com.shop.service;

import org.junit.jupiter.api.Test;

import com.shop.model.Cart;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import org.h2.message.DbException;
import org.junit.jupiter.api.BeforeEach;

public class CartServiceTest {

   CartService m_cartService;

   @BeforeEach
   public void initCart() throws Exception
   {
      Connection conn = DriverManager.getConnection("jdbc:h2:mem:shopdb", "sa", "");

      // Schema anlegen
      ProductRepository productRepo = new ProductRepository(conn);

      productRepo.initSchema();
      // Tabelle leeren, damit Tests isoliert sind
      try (var stmt = conn.createStatement()) {
         stmt.execute("DELETE FROM products");
      }

      // Beispieldaten
      seedProducts(productRepo);
      m_cartService = new CartService(productRepo);
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

   // addToCartTests:
   @Test 
   public void addToCart_NegativeOrNullQuantityTest()
   {
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), "ELEC-001", -1));
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), "ELEC-001", 0));
   }

   @Test 
   public void addToCart_InvalidSKUTest()
   {
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), "INVALID-SKU", 1));
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), "", 1));
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), null, 1));
   }

   @Test 
   public void addToCart_newQuantityTest()
   {
      Cart cart;
      try {
         cart = m_cartService.addToCart(new ArrayList<>(), "ELEC-001", 1);
         cart = m_cartService.addToCart(cart.getItems(), "ELEC-001", 5);
         assertEquals(6, cart.getItems().get(0).getQuantity());
      } catch (SQLException e) {
         assertTrue(false);
      }
   }

   ///////// make test
   @Test 
   public void addToCart_MaxQuantityTest()
   {
      Cart cart;
      try {
         cart = m_cartService.addToCart(new ArrayList<>(), "ELEC-001", 51);
         assertEquals(6, cart.getItems().get(0).getQuantity());
      } catch (SQLException e) {
         assertTrue(false);
      }
   }

   ////// make test
   public void addToCart_NewMaxQuantityTest()
   {
      Cart cart;
      try {
         cart = m_cartService.addToCart(new ArrayList<>(), "ELEC-001", 50);
         cart = m_cartService.addToCart(cart.getItems(), "ELEC-001", 5);
         assertEquals(6, cart.getItems().get(0).getQuantity());
      } catch (SQLException e) {
         assertTrue(false);
      }
   }




   

}
