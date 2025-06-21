package com.example.minor_secure_programming.utils

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for AuthManager
 * Tests authentication token retrieval and verification functionality
 */
class AuthManagerTest {

    private lateinit var mockSupabaseManager: SupabaseManager
    private lateinit var mockAuth: Auth
    
    @Before
    fun setup() {
        // Mock the Supabase manager and Auth objects
        mockSupabaseManager = mock()
        mockAuth = mock()
        
        // Setup reflection to inject our mocks
        val supabaseManagerField = SupabaseManager::class.java.getDeclaredField("INSTANCE")
        supabaseManagerField.isAccessible = true
        val originalManager = supabaseManagerField.get(null)
        supabaseManagerField.set(null, mockSupabaseManager)
        
        // Setup client mock
        val clientField = SupabaseManager::class.java.getDeclaredField("client")
        clientField.isAccessible = true
        val mockClient: Any = mock()
        clientField.set(mockSupabaseManager, mockClient)
        
        // Mock the Auth property
        whenever(mockSupabaseManager.client.auth).thenReturn(mockAuth)
        
        // Store original manager to restore after tests
        afterTest {
            supabaseManagerField.set(null, originalManager)
        }
    }
    
    /**
     * Test getting current token when authenticated
     */
    @Test
    fun getCurrentToken_whenAuthenticated_returnsToken() {
        // Arrange
        val mockSession: io.github.jan.supabase.auth.AuthSession = mock()
        whenever(mockSession.accessToken).thenReturn("test.jwt.token")
        whenever(mockAuth.currentSessionOrNull()).thenReturn(mockSession)
        
        // Act
        val token = AuthManager.getCurrentToken()
        
        // Assert
        assertNotNull("Token should not be null when authenticated", token)
        assertEquals("test.jwt.token", token)
    }
    
    /**
     * Test getting current token when not authenticated
     */
    @Test
    fun getCurrentToken_whenNotAuthenticated_returnsNull() {
        // Arrange
        whenever(mockAuth.currentSessionOrNull()).thenReturn(null)
        
        // Act
        val token = AuthManager.getCurrentToken()
        
        // Assert
        assertNull("Token should be null when not authenticated", token)
    }
    
    /**
     * Test isAuthenticated when user is logged in
     */
    @Test
    fun isAuthenticated_whenUserLoggedIn_returnsTrue() {
        // Arrange
        val mockUser: io.github.jan.supabase.auth.User = mock()
        whenever(mockAuth.currentUserOrNull()).thenReturn(mockUser)
        
        // Act
        val result = AuthManager.isAuthenticated()
        
        // Assert
        assertTrue("Should return true when user is authenticated", result)
    }
    
    /**
     * Test isAuthenticated when user is not logged in
     */
    @Test
    fun isAuthenticated_whenUserNotLoggedIn_returnsFalse() {
        // Arrange
        whenever(mockAuth.currentUserOrNull()).thenReturn(null)
        
        // Act
        val result = AuthManager.isAuthenticated()
        
        // Assert
        assertFalse("Should return false when user is not authenticated", result)
    }
}

// Helper function to register cleanup after tests
private inline fun afterTest(crossinline block: () -> Unit) {
    Runtime.getRuntime().addShutdownHook(Thread {
        block()
    })
}
