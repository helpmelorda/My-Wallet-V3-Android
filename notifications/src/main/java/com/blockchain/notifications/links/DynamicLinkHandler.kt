package com.blockchain.notifications.links

import android.content.Intent
import android.net.Uri
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import io.reactivex.rxjava3.core.Maybe

interface PendingLink {
    fun getPendingLinks(intent: Intent): Maybe<Uri>
}

internal class DynamicLinkHandler internal constructor(
    private val dynamicLinks: FirebaseDynamicLinks
) : PendingLink {

    override fun getPendingLinks(intent: Intent): Maybe<Uri> = Maybe.create { emitter ->
        dynamicLinks.getDynamicLink(intent)
            .addOnSuccessListener { linkData ->
                if (!emitter.isDisposed) {
                    linkData?.link?.let {
                        val emitterData = checkData(it, intent)
                        emitter.onSuccess(emitterData)
                    } ?: emitter.onComplete()
                }
            }
            .addOnFailureListener { if (!emitter.isDisposed) emitter.onError(it) }
    }

    private fun checkData(uri: Uri, originalIntent: Intent): Uri =
        when {
            uri.isOpenBankingLink() -> {
                Uri.parse(
                    "$uri$OPEN_BANKING_CONSENT_QUERY${
                        originalIntent.data?.getQueryParameter(
                            OPEN_BANKING_CONSENT_VALUE
                        ) ?: "BTC"
                    }"
                )
            }
            uri.isTickerQueryParamSupportedUri() -> Uri.parse(
                "$uri$DEEPLINK_TICKER_QUERY${
                    originalIntent.data?.getQueryParameter(
                        TICKER_QUERY_PARAM
                    ) ?: ""
                }"
            )
            uri.isCampaignTypeQueryParamSupportedUri() -> Uri.parse(
                "$uri$DEEPLINK_CAMPAIGN_QUERY${originalIntent.data?.getQueryParameter(CAMPAIGN_QUERY_PARAM)}"
            )
            else -> uri
        }

    private fun Uri.isOpenBankingLink() =
        fragment?.contains(OPEN_BANKING_APPROVAL) == true ||
            fragment?.contains(OPEN_BANKING_LINK) == true

    private fun Uri.isCampaignTypeQueryParamSupportedUri() =
        fragment?.contains(DEEPLINK_KYC) == true

    private fun Uri.isTickerQueryParamSupportedUri() =
        fragment?.contains(DEEPLINK_BUY) == true ||
            fragment?.contains(DEEPLINK_SELL) == true ||
            fragment?.contains(DEEPLINK_SIMPLEBUY) == true ||
            fragment?.contains(DEEPLINK_SEND) == true ||
            fragment?.contains(DEEPLINK_RECEIVE) == true

    companion object {
        private const val OPEN_BANKING_LINK = "ob-bank-link"
        private const val OPEN_BANKING_APPROVAL = "ob-bank-approval"
        private const val OPEN_BANKING_CONSENT_QUERY = "?one-time-token="
        private const val OPEN_BANKING_CONSENT_VALUE = "one-time-token"
        private const val DEEPLINK_BUY = "open/buy"
        private const val DEEPLINK_SELL = "open/sell"
        private const val DEEPLINK_SIMPLEBUY = "open/simplebuy"
        private const val DEEPLINK_SEND = "open/send"
        private const val DEEPLINK_RECEIVE = "open/receive"
        private const val DEEPLINK_KYC = "open/kyc"
        private const val DEEPLINK_TICKER_QUERY = "?ticker="
        private const val TICKER_QUERY_PARAM = "ticker"
        private const val DEEPLINK_CAMPAIGN_QUERY = "?campaignType="
        private const val CAMPAIGN_QUERY_PARAM = "campaignType"
    }
}