package com.example.techappnew.network


data class AddRequest(
    val email:String,
    val login: String,
    val password: String,
    val admin:Boolean = false
)