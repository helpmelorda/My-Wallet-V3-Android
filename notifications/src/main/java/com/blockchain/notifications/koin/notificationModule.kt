package com.blockchain.notifications.koin

import android.app.NotificationManager
import android.content.Context
import com.blockchain.notifications.CrashLoggerImpl
import com.blockchain.koin.nabu
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.logging.CrashLogger
import com.blockchain.logging.EventLogger
import com.blockchain.notifications.BuildConfig
import com.blockchain.notifications.NotificationService
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.AnalyticsImpl
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.InjectableLogging
import com.blockchain.notifications.analytics.UserAnalytics
import com.blockchain.notifications.analytics.UserAnalyticsImpl
import com.blockchain.notifications.links.DynamicLinkHandler
import com.blockchain.notifications.links.PendingLink
import com.blockchain.remoteconfig.ABTestExperiment
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.RemoteConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {

    scope(payloadScopeQualifier) {
        scoped {
            NotificationTokenManager(
                notificationService = get(),
                payloadManager = get(),
                prefs = get(),
                firebaseInstanceId = get(),
                rxBus = get(),
                crashLogger = get()
            )
        }
    }

    single { FirebaseInstanceId.getInstance() }

    single { FirebaseAnalytics.getInstance(get()) }

    factory { NotificationService(get()) }

    factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) }.bind(NotificationManager::class)

    single { FirebaseDynamicLinks.getInstance() }

    factory { DynamicLinkHandler(get()) }.bind(PendingLink::class)

    factory {
        AnalyticsImpl(
            firebaseAnalytics = get(),
            nabuAnalytics = get(nabu),
            store = get()
        )
    }.bind(Analytics::class)

    factory { UserAnalyticsImpl(get()) }
        .bind(UserAnalytics::class)

    factory { InjectableLogging(get()) }.bind(EventLogger::class)

    single {
        val config = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettings(config)
        }
    }

    factory {
        RemoteConfiguration(
            remoteConfig = get()
        )
    }.bind(RemoteConfig::class)
        .bind(ABTestExperiment::class)

    single {
        CrashLoggerImpl(BuildConfig.DEBUG)
    }.bind(CrashLogger::class)
}