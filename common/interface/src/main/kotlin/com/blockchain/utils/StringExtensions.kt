package com.blockchain.utils

fun String.capitalizeFirstChar(): String =
    this.lowercase().replaceFirstChar { it.uppercase() }