@file:UseSerializers(BigDecimalSerializer::class)

package com.blockchain.api.services

import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.analytics.AnalyticsContext
import com.blockchain.api.analytics.AnalyticsRequestBody
import com.blockchain.api.serializers.BigDecimalSerializer
import io.reactivex.rxjava3.core.Completable
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal

class AnalyticsService internal constructor(
    private val api: AnalyticsApiInterface
) {
    fun postEvents(
        events: List<NabuAnalyticsEvent>,
        id: String,
        analyticsContext: AnalyticsContext,
        platform: String,
        device: String,
        authorization: String?
    ): Completable {

        return api.postAnalytics(
            authorization,
            AnalyticsRequestBody(
                id = id,
                device = device,
                platform = platform,
                events = events,
                context = analyticsContext
            )
        )
    }
}

@Serializable
data class NabuAnalyticsEvent(
    val name: String,
    val type: String,
    val originalTimestamp: String,
    val properties: Map<String, String>,
    val numericProperties: Map<String, BigDecimal>
)
