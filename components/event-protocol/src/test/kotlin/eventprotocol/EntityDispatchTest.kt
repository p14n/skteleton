package eventprotocol

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for EntityDispatch (User Story 7).
 * 
 * These tests verify that EntityDispatch correctly implements polymorphic
 * entity updates using a map-based multimethod pattern.
 */
class EntityDispatchTest {
    
    // Test entity types
    data class User(val id: String, val email: String, val status: String)
    data class Order(val id: String, val userId: String, val total: Double, val status: String)
    data class Product(val id: String, val name: String, val price: Double)
    
    @Test
    fun `test EntityDispatch dispatches to correct updater by entity type`() {
        // Arrange
        var userUpdaterCalled = false
        var orderUpdaterCalled = false
        
        val userUpdater: (User, BaseEvent) -> User = { user, event ->
            userUpdaterCalled = true
            assertEquals("user-123", user.id)
            assertEquals("user.updated", event.type)
            user.copy(status = "active")
        }
        
        val orderUpdater: (Order, BaseEvent) -> Order = { order, event ->
            orderUpdaterCalled = true
            assertEquals("order-456", order.id)
            order.copy(status = "shipped")
        }
        
        val dispatch = EntityDispatch.create<Any>()
            .register(User::class, userUpdater)
            .register(Order::class, orderUpdater)
        
        val userEvent = BaseEvent(
            eventId = "evt-001",
            type = "user.updated",
            data = emptyMap()
        )
        
        val user = User(id = "user-123", email = "user@example.com", status = "pending")
        
        // Act
        val updatedUser = dispatch.update(user, userEvent) as User
        
        // Assert
        assertTrue(userUpdaterCalled, "User updater should have been called")
        assertFalse(orderUpdaterCalled, "Order updater should not have been called")
        assertEquals("active", updatedUser.status)
    }
    
    @Test
    fun `test EntityDispatch handles multiple entity types`() {
        // Arrange
        val userUpdater: (User, BaseEvent) -> User = { user, event ->
            val newEmail = event.data["email"] as? String ?: user.email
            user.copy(email = newEmail)
        }
        
        val orderUpdater: (Order, BaseEvent) -> Order = { order, event ->
            val newTotal = event.data["total"] as? Double ?: order.total
            order.copy(total = newTotal)
        }
        
        val productUpdater: (Product, BaseEvent) -> Product = { product, event ->
            val newPrice = event.data["price"] as? Double ?: product.price
            product.copy(price = newPrice)
        }
        
        val dispatch = EntityDispatch.create<Any>()
            .register(User::class, userUpdater)
            .register(Order::class, orderUpdater)
            .register(Product::class, productUpdater)
        
        // Act & Assert - User
        val userEvent = BaseEvent(
            eventId = "evt-user",
            type = "user.updated",
            data = mapOf("email" to "newemail@example.com")
        )
        val user = User(id = "user-1", email = "old@example.com", status = "active")
        val updatedUser = dispatch.update(user, userEvent) as User
        assertEquals("newemail@example.com", updatedUser.email)
        
        // Act & Assert - Order
        val orderEvent = BaseEvent(
            eventId = "evt-order",
            type = "order.updated",
            data = mapOf("total" to 199.99)
        )
        val order = Order(id = "order-1", userId = "user-1", total = 99.99, status = "pending")
        val updatedOrder = dispatch.update(order, orderEvent) as Order
        assertEquals(199.99, updatedOrder.total)
        
        // Act & Assert - Product
        val productEvent = BaseEvent(
            eventId = "evt-product",
            type = "product.updated",
            data = mapOf("price" to 29.99)
        )
        val product = Product(id = "product-1", name = "Widget", price = 19.99)
        val updatedProduct = dispatch.update(product, productEvent) as Product
        assertEquals(29.99, updatedProduct.price)
    }
    
    @Test
    fun `test EntityDispatch throws exception for unregistered entity type`() {
        // Arrange
        data class UnregisteredEntity(val id: String)
        
        val userUpdater: (User, BaseEvent) -> User = { user, _ -> user }
        val dispatch = EntityDispatch.create<Any>()
            .register(User::class, userUpdater)
        
        val event = BaseEvent(
            eventId = "evt-001",
            type = "test.event",
            data = emptyMap()
        )
        val unregisteredEntity = UnregisteredEntity(id = "test-1")
        
        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            dispatch.update(unregisteredEntity, event)
        }
        
        assertTrue(exception.message?.contains("No updater registered") == true)
    }
    
    @Test
    fun `test EntityDispatch builder pattern allows chaining`() {
        // Arrange
        val userUpdater: (User, BaseEvent) -> User = { user, _ -> user }
        val orderUpdater: (Order, BaseEvent) -> Order = { order, _ -> order }
        val productUpdater: (Product, BaseEvent) -> Product = { product, _ -> product }
        
        // Act - Chain multiple registrations
        val dispatch = EntityDispatch.create<Any>()
            .register(User::class, userUpdater)
            .register(Order::class, orderUpdater)
            .register(Product::class, productUpdater)
        
        // Assert - All types should be registered
        val user = User(id = "u1", email = "test@example.com", status = "active")
        val order = Order(id = "o1", userId = "u1", total = 100.0, status = "pending")
        val product = Product(id = "p1", name = "Test", price = 50.0)
        val event = BaseEvent(eventId = "evt", type = "test", data = emptyMap())
        
        assertDoesNotThrow { dispatch.update(user, event) }
        assertDoesNotThrow { dispatch.update(order, event) }
        assertDoesNotThrow { dispatch.update(product, event) }
    }
}

