package com.blockchain.koin.modules

import com.blockchain.data.activity.historicRate.HistoricRateFetcher
import com.blockchain.data.activity.historicRate.HistoricRateLocalSource
import com.blockchain.data.activity.historicRate.HistoricRateRemoteSource
import com.blockchain.koin.payloadScopeQualifier
import org.koin.dsl.module

val featureActivitiesModule = module {
    scope(payloadScopeQualifier) {
        scoped {
            HistoricRateLocalSource(get())
        }

        scoped {
            HistoricRateRemoteSource(get())
        }

        scoped {
            HistoricRateFetcher(get(), get())
        }
    }
}
