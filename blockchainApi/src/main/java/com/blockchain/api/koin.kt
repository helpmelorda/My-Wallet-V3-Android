package com.blockchain.api

import com.blockchain.api.addressmapping.AddressMappingApiInterface
import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.bitcoin.BitcoinApiInterface
import com.blockchain.api.nabu.NabuUserApiInterface
import com.blockchain.api.wallet.WalletApiInterface
import com.blockchain.api.custodial.CustodialBalanceApiInterface
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.core.qualifier.StringQualifier
import org.koin.core.scope.Scope
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory

val blockchainApi = StringQualifier("blockchain-api")
val explorerApi = StringQualifier("explorer-api")
val nabuApi = StringQualifier("nabu-api")

val blockchainApiModule = module {

    single { RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()) }

    single(blockchainApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("blockchain-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    single(explorerApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("explorer-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    single(nabuApi) {
        Retrofit.Builder()
            .baseUrl(getBaseUrl("nabu-api"))
            .client(get())
            .addCallAdapterFactory(get<RxJava3CallAdapterFactory>())
            .addConverterFactory(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }.asConverterFactory("application/json".toMediaType())
            )
            .build()
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(BitcoinApiInterface::class.java)
        NonCustodialBitcoinService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(explorerApi).create(WalletApiInterface::class.java)
        WalletSettingsService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AddressMappingApiInterface::class.java)
        AddressMappingService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AnalyticsApiInterface::class.java)
        AnalyticsService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(NabuUserApiInterface::class.java)
        NabuUserService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(CustodialBalanceApiInterface::class.java)
        CustodialBalanceService(
            api = api
        )
    }
}

fun Scope.getBaseUrl(propName: String): String = getProperty(propName)
