package com.blockchain.api

import com.blockchain.api.addressmapping.AddressMappingApiInterface
import com.blockchain.api.analytics.AnalyticsApiInterface
import com.blockchain.api.assetprice.AssetPriceApiInterface
import com.blockchain.api.bitcoin.BitcoinApi
import com.blockchain.api.nabu.NabuUserApi
import com.blockchain.api.wallet.WalletApiInterface
import com.blockchain.api.ethereum.EthereumApiInterface
import com.blockchain.api.custodial.CustodialBalanceApi
import com.blockchain.api.interest.InterestApiInterface
import com.blockchain.api.services.AddressMappingService
import com.blockchain.api.services.AnalyticsService
import com.blockchain.api.services.AssetPriceService
import com.blockchain.api.services.CustodialBalanceService
import com.blockchain.api.services.InterestService
import com.blockchain.api.services.NabuUserService
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.services.NonCustodialErc20Service
import com.blockchain.api.services.TradeService
import com.blockchain.api.services.WalletSettingsService
import com.blockchain.api.trade.TradeApi
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
        val api = get<Retrofit>(blockchainApi).create(BitcoinApi::class.java)
        NonCustodialBitcoinService(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(EthereumApiInterface::class.java)
        NonCustodialErc20Service(
            api,
            getProperty("api-code")
        )
    }

    factory {
        val api = get<Retrofit>(blockchainApi).create(AssetPriceApiInterface::class.java)
        AssetPriceService(
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
        val api = get<Retrofit>(nabuApi).create(NabuUserApi::class.java)
        NabuUserService(
            api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(CustodialBalanceApi::class.java)
        CustodialBalanceService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(InterestApiInterface::class.java)
        InterestService(
            api = api
        )
    }

    factory {
        val api = get<Retrofit>(nabuApi).create(TradeApi::class.java)
        TradeService(
            api = api
        )
    }
}

fun Scope.getBaseUrl(propName: String): String = getProperty(propName)
