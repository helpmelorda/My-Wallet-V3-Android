package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.network.modules.OkHttpInterceptors
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.facebook.stetho.okhttp3.StethoInterceptor
import info.blockchain.wallet.api.Environment
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.api.interceptors.ApiInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.DeviceIdInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.RequestIdInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.SSLPinningInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.UserAgentInterceptor
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.util.UUID

val apiInterceptorsModule = module {

    single {
        val env: EnvironmentConfig = get()
        val versionName = BuildConfig.VERSION_NAME.removeSuffix(BuildConfig.VERSION_NAME_SUFFIX)
        OkHttpInterceptors(
            mutableListOf(
                SSLPinningInterceptor(get()),
                UserAgentInterceptor(versionName, Build.VERSION.RELEASE),
                DeviceIdInterceptor(prefs = lazy { get<PersistentPrefs>() }, get()),
                RequestIdInterceptor { UUID.randomUUID().toString() }
            ).apply {
                if (env.isRunningInDebugMode()) {
                    add(StethoInterceptor())
                    add(ApiInterceptor())
                    if (env.environment != Environment.PRODUCTION) {
                        add(ChuckerInterceptor.Builder(androidContext()).build())
                    }
                }
            }
        )
    }
}
