package com.benjakusa.malele.domain.model

data class User(
    val id: String,
    val email: String,
    val role: String,
    val displayName: String?,
    val emailVerified: Boolean,
    val isActive: Boolean,
)

sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val code: String, val message: String) : AuthResult()
}
