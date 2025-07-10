package com.example.techappnew.network


data class CumpusRequest(
    val campus_id: Int,
    val campus_number: Int,
    val address: String,
    val rooms: List<Room> = emptyList()
){
    override fun toString(): String = address ?: "Корпус $campus_number"
}
data class Room(
    val classroom_id: Int,
    val classroom_number: Int,
    val campus_id: Int
) {
    fun getFormattedNumber(): String = "$classroom_number"
    override fun toString(): String = getFormattedNumber()
}
