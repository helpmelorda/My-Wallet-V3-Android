package piuk.blockchain.android.deeplink

import android.net.Uri
import piuk.blockchain.android.kyc.ignoreFragment

class BlockchainDeepLinkParser {
    private fun getQueryParameters(uri: Uri, stateClass: Class<*>): Map<String, String>  {
        val hashMap = hashMapOf<String, String>()
        for (prop in stateClass.declaredFields) {
            uri.ignoreFragment().getQueryParameter(prop.name)?.let {
                hashMap[prop.name] = it
            }
        }
        return hashMap.toMap()
    }

    fun mapUri(uri: Uri): BlockchainLinkState {
        return when (uri.ignoreFragment().path) {
            "/swap" -> {
                BlockchainLinkState.Swap
            }
            "/activities" -> {
                BlockchainLinkState.Activities
            }
            "/buy" -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.Buy::class.java)
                BlockchainLinkState.Buy(ticker = queries["ticker"])
            }
            "/sell" -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.Sell::class.java)
                BlockchainLinkState.Sell(ticker = queries["ticker"])
            }
            "/simplebuy" -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.SimpleBuy::class.java)
                val ticker = queries["ticker"] ?: return BlockchainLinkState.NoUri
                BlockchainLinkState.SimpleBuy(ticker = ticker)
            }
            "/kyc" -> {
                val queries = getQueryParameters(uri, BlockchainLinkState.KycCampaign::class.java)
                BlockchainLinkState.KycCampaign(campaignType = queries["campaignType"].orEmpty())
            }
            "/twofa" -> {
                BlockchainLinkState.TwoFa
            }
            "/verifyemail" -> {
                BlockchainLinkState.VerifyEmail
            }
            "/setupfingerprint" -> {
                BlockchainLinkState.SetupFingerprint
            }
            "/interest" -> {
                BlockchainLinkState.Interest
            }
            "/receive" -> {
                BlockchainLinkState.Receive
            }
            "/send" -> {
                BlockchainLinkState.Send
            }
            else -> BlockchainLinkState.NoUri
        }
    }
}

sealed class BlockchainLinkState {
    object NoUri : BlockchainLinkState()
    object Swap : BlockchainLinkState()
    object Activities : BlockchainLinkState()
    object Interest : BlockchainLinkState()
    object TwoFa : BlockchainLinkState()
    object VerifyEmail : BlockchainLinkState()
    object SetupFingerprint : BlockchainLinkState()
    object Receive : BlockchainLinkState()
    object Send : BlockchainLinkState()
    data class Buy(val ticker: String? = null) : BlockchainLinkState()
    data class Sell(val ticker: String? = null) : BlockchainLinkState()
    data class KycCampaign(val campaignType: String) : BlockchainLinkState()
    data class SimpleBuy(val ticker: String) : BlockchainLinkState()
}