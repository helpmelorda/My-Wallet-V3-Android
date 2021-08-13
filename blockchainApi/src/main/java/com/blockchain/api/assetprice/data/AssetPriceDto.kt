package com.blockchain.api.assetprice.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PriceSymbolDto(
    @SerialName("code")
    val code: String, // "UNI"/"CAD"
    @SerialName("symbol")
    val ticker: String, // "UNI"/"CAD"
    @SerialName("description")
    val name: String, // "Uniswap"/"Canadian Dollar"
    @SerialName("decimals")
    val precisionDp: Int, // 18/2
    @SerialName("fiat")
    val isFiat: Boolean // true/false
)

@Serializable
data class AvailableSymbolsDto(
    @SerialName("Base")
    val baseSymbols: Map<String, PriceSymbolDto>,
    @SerialName("Quote")
    val quoteSymbols: Map<String, PriceSymbolDto>
)

@Serializable
internal data class AssetPriceDto(
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("price")
    val price: Double?,
    @SerialName("volume24h")
    val volume24h: Double?
)

@Serializable
internal data class PriceRequestPairDto(
    @SerialName("base")
    val crypto: String,
    @SerialName("quote")
    val fiat: String
)
