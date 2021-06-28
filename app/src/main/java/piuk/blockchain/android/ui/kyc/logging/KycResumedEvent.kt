package piuk.blockchain.android.ui.kyc.logging

import piuk.blockchain.android.ui.kyc.reentry.ReentryPoint
import com.blockchain.notifications.analytics.LoggingEvent

fun kycResumedEvent(entryPoint: ReentryPoint) =
    LoggingEvent("User Resumed KYC flow", mapOf("User resumed KYC" to entryPoint.entryPoint))
