package piuk.blockchain.android.coincore

import com.blockchain.koin.payloadScope
import com.blockchain.koin.payloadScopeQualifier
import info.blockchain.balance.AssetCatalogue
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.coincore.bch.BchAsset
import piuk.blockchain.android.coincore.btc.BtcAsset
import piuk.blockchain.android.coincore.eth.EthAsset
import piuk.blockchain.android.coincore.fiat.FiatAsset
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.coincore.impl.AssetCatalogueImpl
import piuk.blockchain.android.coincore.impl.AssetLoader
import piuk.blockchain.android.coincore.impl.CryptoAssetLoader
import piuk.blockchain.android.coincore.impl.BackendNotificationUpdater
import piuk.blockchain.android.coincore.impl.TxProcessorFactory
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.android.coincore.xlm.XlmAsset
import piuk.blockchain.android.repositories.AssetActivityRepository

val coincoreModule = module {

    scope(payloadScopeQualifier) {

        scoped {
            BtcAsset(
                exchangeRates = get(),
                sendDataManager = get(),
                feeDataManager = get(),
                historicRates = get(),
                currencyPrefs = get(),
                payloadManager = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                notificationUpdater = get(),
                coinsWebsocket = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            BchAsset(
                payloadManager = get(),
                bchDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                feeDataManager = get(),
                sendDataManager = get(),
                pitLinking = get(),
                labels = get(),
                walletPreferences = get(),
                beNotifyUpdate = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            XlmAsset(
                payloadManager = get(),
                xlmDataManager = get(),
                xlmFeesFetcher = get(),
                walletOptionsDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                pitLinking = get(),
                crashLogger = get(),
                labels = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }.bind(CryptoAsset::class)

        scoped {
            EthAsset(
                payloadManager = get(),
                ethDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                walletPrefs = get(),
                crashLogger = get(),
                custodialManager = get(),
                pitLinking = get(),
                labels = get(),
                notificationUpdater = get(),
                identity = get(),
                features = get(),
                assetCatalogue = lazy { get() }
            )
        }.bind(CryptoAsset::class)

        scoped {
            FiatAsset(
                labels = get(),
                custodialAssetWalletsBalancesRepository = get(),
                exchangeRateDataManager = get(),
                custodialWalletManager = get(),
                currencyPrefs = get()
            )
        }

        scoped {
            Coincore(
                payloadManager = get(),
                fiatAsset = get<FiatAsset>(),
                assetLoader = get(),
                assetCatalogue = get(),
                txProcessorFactory = get(),
                defaultLabels = get(),
                crashLogger = get()
            )
        }

        scoped {
            val nonErc20Assets: List<CryptoAsset> = payloadScope.getAll()
            CryptoAssetLoader(
                fixedAssets = nonErc20Assets,
                assetCatalogue = get(),
                payloadManager = get(),
                ethDataManager = get(),
                feeDataManager = get(),
                exchangeRates = get(),
                historicRates = get(),
                currencyPrefs = get(),
                custodialManager = get(),
                crashLogger = get(),
                labels = get(),
                pitLinking = get(),
                walletPreferences = get(),
                identity = get(),
                features = get()
            )
        }.bind(AssetLoader::class)

        scoped {
            TxProcessorFactory(
                bitPayManager = get(),
                exchangeRates = get(),
                walletManager = get(),
                walletPrefs = get(),
                quotesEngine = get(),
                analytics = get(),
                kycTierService = get()
            )
        }

        scoped {
            AssetActivityRepository(
                coincore = get()
            )
        }

        scoped {
            AddressFactoryImpl(
                coincore = get(),
                addressResolver = get(),
                features = get()
            )
        }.bind(AddressFactory::class)

        scoped {
            BackendNotificationUpdater(
                prefs = get(),
                walletApi = get()
            )
        }

        factory {
            TransferQuotesEngine(quotesProvider = get())
        }

        factory {
            LinkedBanksFactory(
                custodialWalletManager = get()
            )
        }

        factory {
            SwapTrendingPairsProvider(
                coincore = get(),
                identity = get()
            )
        }.bind(TrendingPairsProvider::class)
    }

    single {
        AssetCatalogueImpl(
            remoteConfig = get()
        )
    }.bind(AssetCatalogue::class)
}