package com.example.techappnew.mask

data class Problem(
    val problem_id: Int,
    val description: String,
    val active: Boolean,
    val status: String,
    val device_id: Int,
    val user_id: Int,
    val created_at: String,
    val updated_at: String
)