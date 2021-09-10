package com.blockchain.common.util

import java.util.Locale

fun ByteArray.toHex(): String =
    StringBuilder(size * 2).also { builder ->
        forEach { byte ->
            builder.append(String.format(Locale.US, "%02X", byte))
        }
    }.toString()