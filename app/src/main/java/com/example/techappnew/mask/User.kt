package com.example.techappnew

data class User(
    val user_id: Int,
    val email: String,
    val login: String,
    val admin: Boolean,
    val password: String
)