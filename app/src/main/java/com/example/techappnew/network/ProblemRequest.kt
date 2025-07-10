package com.example.techappnew.network

import com.google.gson.Gson


data class ProblemRequest(
    val device_id: Int,
    val user_id: Int,
    val description: String,
    val active: Boolean = true,
    val status: String = "Pending"
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
}