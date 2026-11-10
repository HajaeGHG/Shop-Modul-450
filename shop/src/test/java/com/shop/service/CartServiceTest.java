package com.shop.service;

import org.junit.jupiter.api.Test;

import com.shop.model.Cart;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.refEq;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.shop.model.CartItem;
import com.shop.exception.InsufficientStockException;
import java.sql.SQLException;

public class CartServiceTest {

   private CartService m_cartService;
   static String m_skus[] = {"ELEC-001", "ELEC-002", "ELEC-003", "BOOK-001", "BOOK-002", "CLOTH-001", "FOOD-001", "FOOD-002"};
   
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

  @Test
  public void removeFromCartWhenSkuExistsTest() throws Exception {
    // Arrange
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);

    cart = m_cartService.addToCart(cart.items(), "ELEC-011", 2);
    cart = m_cartService.addToCart(cart.items(), "BOOK-011", 6);
    cart = m_cartService.addToCart(cart.items(), "FOOD-011", 1);

    assertEquals(3, cart.items().size());

    // Act
    cart = m_cartService.removeFromCart(cart.items(), "BOOK-011");

    // Assert
    assertEquals(2, cart.items().size());
    assertTrue(cart.items().stream().noneMatch(i -> i.product().sku().equals("BOOK-011")));
  }

  @Test
  public void removeFromCartWhenSkuDoesNotExistTest() throws Exception {
    // Arrange
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);

    cart = m_cartService.addToCart(cart.items(), "ELEC-011", 2);
    cart = m_cartService.addToCart(cart.items(), "BOOK-011", 6);
    cart = m_cartService.addToCart(cart.items(), "FOOD-011", 1);

    assertEquals(3, cart.items().size());

    // Act
    cart = m_cartService.removeFromCart(cart.items(), "NON-EXISTENT-SKU");

    // Assert
    assertEquals(3, cart.items().size());
  }

  @Test
  public void removeFromCartWhenCartIsEmptyTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);

    assertEquals(0, cart.items().size());

    cart = m_cartService.removeFromCart(cart.items(), "BOOK-011");

    assertEquals(0, cart.items().size());
  }

  @Test
  public void updateQuantityIsGoodTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    cart = m_cartService.addToCart(cart.items(), "ELEC-011", 1);
    assertEquals(1, cart.items().size());
    assertEquals(1, cart.items().get(0).quantity());

    cart = m_cartService.updateQuantity(cart.items(), "ELEC-011", 4);

    assertEquals(1, cart.items().size());
    assertEquals(4, cart.items().get(0).quantity());
  }

  @Test
  public void updateQuantityToZeroTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    cart = m_cartService.addToCart(cart.items(), "ELEC-011", 1);
    assertEquals(1, cart.items().size());
    assertEquals(1, cart.items().get(0).quantity());

    cart = m_cartService.updateQuantity(cart.items(), "ELEC-011", 0);

    assertEquals(0, cart.items().size());
  }

  @Test
  public void quantityIsAboveMaxQuantityPerItemTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    assertThrows(IllegalArgumentException.class, () -> {
      m_cartService.addToCart(cart.items(), "ELEC-011", 21);
    });
  }

  @Test
  public void productDoesNotExistTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    assertThrows(IllegalArgumentException.class, () -> {
      m_cartService.addToCart(cart.items(), "TEST-011", 1);
    });
  }

  @Test
  public void productIsOutOfStockTest() throws Exception {
    List<CartItem> items = new ArrayList<>();
    Cart cart = new Cart(items);
    assertThrows(InsufficientStockException.class, () -> {
      m_cartService.addToCart(cart.items(), "CLOTH-011", 1);
    });
  }

   private static void seedProducts(ProductRepository repo) throws Exception {
      repo.save(new Product(null, m_skus[0], "USB-C Kabel 2m", new BigDecimal("19.90"), 100, Product.Category.ELECTRONICS));
      repo.save(new Product(null, m_skus[1], "Bluetooth Kopfhörer", new BigDecimal("79.00"), 30, Product.Category.ELECTRONICS));
      repo.save(new Product(null, m_skus[2], "Webcam HD", new BigDecimal("45.50"), 15, Product.Category.ELECTRONICS));
      repo.save(new Product(null, m_skus[3], "Clean Code", new BigDecimal("42.00"), 50, Product.Category.BOOKS));
      repo.save(new Product(null, m_skus[4], "Domain-Driven Design", new BigDecimal("55.00"), 20, Product.Category.BOOKS));
      repo.save(new Product(null, m_skus[5], "T-Shirt Bio", new BigDecimal("24.90"), 200, Product.Category.CLOTHING));
      repo.save(new Product(null, m_skus[6], "Schoggi 200g", new BigDecimal("8.50"), 500, Product.Category.FOOD));
      repo.save(new Product(null, m_skus[7], "Kaffee Bohnen 1kg", new BigDecimal("18.00"), 80, Product.Category.FOOD));
      repo.save(new Product(null, "ELEC-011", "USB-C Kabel 2m", new BigDecimal("19.90"), 100, Product.Category.ELECTRONICS));
      repo.save(new Product(null, "BOOK-011", "Clean Code", new BigDecimal("42.00"), 50, Product.Category.BOOKS));
      repo.save(new Product(null, "FOOD-011", "Schoggi 200g", new BigDecimal("8.50"), 500, Product.Category.FOOD));
      repo.save(new Product(null, "CLOTH-011", "T-Shirt Gr. M", new BigDecimal("25.00"), 0, Product.Category.CLOTHING));
      refEq(null, m_skus);
   }

   // addToCartTests:
   @Test 
   public void addToCart_NegativeOrNullQuantityTest()
   {
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), m_skus[0], -1));
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), m_skus[0], 0));
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
         cart = m_cartService.addToCart(new ArrayList<>(), m_skus[0], 1);
         cart = m_cartService.addToCart(cart.getItems(), m_skus[0], 5);
         assertEquals(6, cart.getItems().get(0).getQuantity());
      } catch (SQLException e) {
         assertTrue(false);
      }
   }

   @Test 
   public void addToCart_MaxQuantityItemTest()
   {
      assertDoesNotThrow(() -> m_cartService.addToCart(new ArrayList<>(), m_skus[0], CartService.getMaxQuantityPerItem() - 1));
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), m_skus[0], CartService.getMaxQuantityPerItem()));
      assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), m_skus[0], CartService.getMaxQuantityPerItem() + 1));
   }

   @Test
   public void addToCart_NewMaxQuantityItemTest()
   {
      Cart cart;
      try {
         cart = m_cartService.addToCart(new ArrayList<>(), m_skus[0], CartService.getMaxQuantityPerItem() / 2);
         assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), m_skus[0], CartService.getMaxQuantityPerItem() / 2 + 1));
         assertEquals(6, cart.getItems().get(0).getQuantity());
      } catch (SQLException e) {
         assertTrue(false);
      }
   }

   @Test
   public void addToCart_MaxQuantityOrderTest()
   {
      for (int i = 0; i < CartService.getMaxQuantityPerOrder() / CartService.getMaxQuantityPerItem() - 1; i++)
      {
         final String sku = m_skus[i];
         if (i >= 7)
            assertTrue(true);
         try {
            if (i < CartService.getMaxQuantityPerOrder() / CartService.getMaxQuantityPerItem())
               m_cartService.addToCart(new ArrayList<>(), sku, CartService.getMaxQuantityPerItem());
            else
               assertThrowsExactly(IllegalArgumentException.class, () -> m_cartService.addToCart(new ArrayList<>(), sku, CartService.getMaxQuantityPerItem()));

         } catch (SQLException e) {
            assertTrue(false);
         }
      }
   }   

}
