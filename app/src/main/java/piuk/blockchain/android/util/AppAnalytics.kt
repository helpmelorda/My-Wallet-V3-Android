package piuk.blockchain.android.util

import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.notifications.analytics.AnalyticsNames
import java.io.Serializable

sealed class AppAnalytics(
    override val event: String,
    override val params: Map<String, Serializable> = emptyMap()
) : AnalyticsEvent {

    object AppInstalled : AppAnalytics(AnalyticsNames.APP_INSTALLED.eventName)
    object AppUpdated : AppAnalytics(AnalyticsNames.APP_UPDATED.eventName)
    object AppBackgrounded : AppAnalytics(AnalyticsNames.APP_BACKGROUNDED.eventName)
    object AppDeepLinked : AppAnalytics(AnalyticsNames.APP_DEEP_LINK_OPENED.eventName)
}