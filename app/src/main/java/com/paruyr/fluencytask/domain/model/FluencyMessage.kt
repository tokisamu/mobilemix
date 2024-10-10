package com.paruyr.fluencytask.domain.model

data class FluencyMessage(
    val content: String,
    val isSent: Boolean, // true if sent by the device, false if received
)