package com.example.minor_secure_programming.models

import kotlinx.serialization.Serializable

/**
 * Represents a user's game
 */
@Serializable
data class Game(
    val id: String = "",
    val user_id: String = "",
    val category_id: String = "",
    val name: String = "",
    val username: String = "",
    val created_at: String? = null,
    val updated_at: String? = null,
    // Optional field to hold the category name for display
    val category_name: String = ""
)

/**
 * Represents a game category
 */
@Serializable
data class GameCategory(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val supported_stats: Boolean = false,
    val created_at: String? = null
)
