package com.blockchain.api.assetdiscovery.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
/* internal */ data class AssetTemplate(
    @SerialName("symbol")
    val ticker: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: AssetType,
    @SerialName("precision")
    val precisionDp: Int,
    @SerialName("products")
    val productSupport: List<String>
)

@Serializable
/* internal */ sealed class AssetType {
    @SerialName("logoPngUrl")
    val logoUrl: String? = null // "https://raw.githubusercontent.com/trustwallet/assets/..../logo.png"

    @SerialName("websiteUrl")
    val websiteUrl: String? = null // "https://terra.money"

    data class L1CryptoAsset(
        //        "name": "COIN",
        @SerialName("minimumOnChainConfirmations")
        val requiredConfirmations: Int
    ) : AssetType()

    data class L2CryptoAsset(
        //        "name": "ERC20",
        @SerialName("parentChain")
        val parentChain: String, // "ETH"
        @SerialName("erc20Address")
        val chainIdentifier: String // "0xd2877702675e6cEb975b4A1dFf9fb7BAF4C91ea9"
    ) : AssetType()

    data class FiatAsset(
        //        "name": "FIAT"
        val unused: String? = null
    ) : AssetType()
}

/* internal */ class SupportedProduct {
    companion object {
        const val PRODUCT_PRIVATE_KEY = "PrivateKey"
    }
}
