package com.example.techappnew.network

data class LoginPOST(
    val success: Boolean,
    val message: String?,
    val user_id: Int
)