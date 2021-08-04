package com.blockchain.koin

import info.blockchain.wallet.api.dust.BchDustService
import info.blockchain.wallet.api.dust.DustApi
import info.blockchain.wallet.api.dust.DustService
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.ethereum.EthEndpoints
import info.blockchain.wallet.metadata.MetadataService
import info.blockchain.wallet.metadata.MetadataInteractor
import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.multiaddress.MultiAddressFactoryBtc
import info.blockchain.wallet.payload.BalanceManagerBch
import info.blockchain.wallet.payload.BalanceManagerBtc
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.PayloadManagerWiper
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val walletModule = module {

    scope(payloadScopeQualifier) {

        scoped { PayloadManager(get(), get(), get(), get(), get()) }

        factory { MultiAddressFactoryBtc(bitcoinApi = get()) }.bind(MultiAddressFactory::class)

        factory { BalanceManagerBtc(bitcoinApi = get()) }

        factory { BalanceManagerBch(bitcoinApi = get()) }
    }

    factory {
        MetadataInteractor(
            metadataService = get()
        )
    }

    single { get<Retrofit>(apiRetrofit).create(MetadataService::class.java) }

    factory {
        BchDustService(
            get<Retrofit>(kotlinApiRetrofit).create(DustApi::class.java),
            get()
        )
    }.bind(DustService::class)

    single {
        object : PayloadManagerWiper {
            override fun wipe() {
                if (!payloadScope.closed) {
                    payloadScope.close()
                }
            }
        }
    }.bind(PayloadManagerWiper::class)

    factory { EthAccountApi(
        get<Retrofit>(kotlinApiRetrofit).create(EthEndpoints::class.java),
        get()
    ) }
}
