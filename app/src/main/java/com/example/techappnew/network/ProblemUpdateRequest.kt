package com.example.techappnew.network

data class ProblemUpdateRequest(
    val description: String? = null,
    val status: String? = null,
    val active: Boolean? = null,
    val device_id: Int? = null,
    val user_id: Int? = null
)