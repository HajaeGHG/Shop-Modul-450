package com.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shop.model.Cart;
import com.shop.model.CartItem;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;
import com.shop.exception.InsufficientStockException;

public class CartServiceTest {

  private CartService cartService;
  private Connection conn;

  @BeforeEach
  void setUp() throws Exception {
    // 1. Frische Test-Datenbankverbindung aufbauen
    conn = DriverManager.getConnection("jdbc:h2:mem:;DB_CLOSE_DELAY=-1", "sa", "");

    // 2. Repository initialisieren und Schema anlegen
    ProductRepository productRepo = new ProductRepository(conn);
    productRepo.initSchema();

    // 3. Notwendige Testprodukte in der Datenbank erfassen (Seed)
    productRepo.save(
        new Product(null, "ELEC-001", "USB-C Kabel 2m", new BigDecimal("19.90"), 100, Product.Category.ELECTRONICS));
    productRepo.save(new Product(null, "BOOK-001", "Clean Code", new BigDecimal("42.00"), 50, Product.Category.BOOKS));
    productRepo.save(new Product(null, "FOOD-001", "Schoggi 200g", new BigDecimal("8.50"), 500, Product.Category.FOOD));
    productRepo
        .save(new Product(null, "CLOTH-001", "T-Shirt Gr. M", new BigDecimal("25.00"), 0, Product.Category.CLOTHING));

    // 4. CartService mit dem befüllten Repository instanziieren
    cartService = new CartService(productRepo);
  }

  @Test
  public void removeFromCartWhenSkuExistsTest() throws Exception {
    // Arrange
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);

    cart = cartService.addToCart(cart.items(), "ELEC-001", 2);
    cart = cartService.addToCart(cart.items(), "BOOK-001", 6);
    cart = cartService.addToCart(cart.items(), "FOOD-001", 1);

    assertEquals(3, cart.items().size());

    // Act
    cart = cartService.removeFromCart(cart.items(), "BOOK-001");

    // Assert
    assertEquals(2, cart.items().size());
    assertTrue(cart.items().stream().noneMatch(i -> i.product().sku().equals("BOOK-001")));
  }

  @Test
  public void removeFromCartWhenSkuDoesNotExistTest() throws Exception {
    // Arrange
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);

    cart = cartService.addToCart(cart.items(), "ELEC-001", 2);
    cart = cartService.addToCart(cart.items(), "BOOK-001", 6);
    cart = cartService.addToCart(cart.items(), "FOOD-001", 1);

    assertEquals(3, cart.items().size());

    // Act
    cart = cartService.removeFromCart(cart.items(), "NON-EXISTENT-SKU");

    // Assert
    assertEquals(3, cart.items().size());
  }

  @Test
  public void removeFromCartWhenCartIsEmptyTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);

    assertEquals(0, cart.items().size());

    cart = cartService.removeFromCart(cart.items(), "BOOK-001");

    assertEquals(0, cart.items().size());
  }

  @Test
  public void updateQuantityIsGoodTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    cart = cartService.addToCart(cart.items(), "ELEC-001", 1);
    assertEquals(1, cart.items().size());
    assertEquals(1, cart.items().get(0).quantity());

    cart = cartService.updateQuantity(cart.items(), "ELEC-001", 4);

    assertEquals(1, cart.items().size());
    assertEquals(4, cart.items().get(0).quantity());
  }

  @Test
  public void updateQuantityToZeroTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    cart = cartService.addToCart(cart.items(), "ELEC-001", 1);
    assertEquals(1, cart.items().size());
    assertEquals(1, cart.items().get(0).quantity());

    cart = cartService.updateQuantity(cart.items(), "ELEC-001", 0);

    assertEquals(0, cart.items().size());
  }

  @Test
  public void quantityIsAboveMaxQuantityPerItemTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    assertThrows(IllegalArgumentException.class, () -> {
      cartService.addToCart(cart.items(), "ELEC-001", 21);
    });
  }

  @Test
  public void productDoesNotExistTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    assertThrows(IllegalArgumentException.class, () -> {
      cartService.addToCart(cart.items(), "TEST-001", 1);
    });
  }

  @Test
  public void productIsOutOfStockTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    assertThrows(InsufficientStockException.class, () -> {
      cartService.addToCart(cart.items(), "CLOTH-001", 1);
    });
  }
}