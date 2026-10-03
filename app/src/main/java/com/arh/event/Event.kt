package com.arh.event.model

data class Event(
    val title: String,
    val category: String,
    val location: String,
    val description: String?,
    val startTime: String
)

