package com.paruyr.fluencytask.domain.model

import java.math.BigInteger

data class MixMessage(
    val content: String,
    val shares: Array<BigInteger>,
    val index: Int, // true if sent by the device, false if received
)
