package piuk.blockchain.android.ui.launcher

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.logging.CrashLogger
import com.blockchain.operations.AppStartUpFlushable
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.androidcore.data.auth.metadata.WalletCredentialsMetadataUpdater
import piuk.blockchain.androidcore.data.metadata.MetadataInitException
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.then

class Prerequisites(
    private val metadataManager: MetadataManager,
    private val settingsDataManager: SettingsDataManager,
    private val coincore: Coincore,
    private val exchangeRates: ExchangeRatesDataManager,
    private val crashLogger: CrashLogger,
    private val simpleBuySync: SimpleBuySyncFactory,
    private val flushables: List<AppStartUpFlushable>,
    private val walletCredentialsUpdater: WalletCredentialsMetadataUpdater,
    private val rxBus: RxBus
) {

    fun initMetadataAndRelatedPrerequisites(): Completable =
        metadataManager.attemptMetadataSetup()
            .logOnError(METADATA_ERROR_MESSAGE)
            .onErrorResumeNext {
                if (it is InvalidCredentialsException || it is HDWalletException) {
                    Completable.error(it)
                } else
                    Completable.error(MetadataInitException(it))
            }.then {
                coincore.init() // Coincore signals the crash logger internally
            }.then {
                simpleBuySync.performSync()
                    .logAndCompleteOnError(SIMPLE_BUY_SYNC)
            }.then {
                Completable.concat(
                    flushables.map { it.flush().logAndCompleteOnError(it.tag) }
                )
            }.then {
                walletCredentialsUpdater.checkAndUpdate()
                    .logAndCompleteOnError(WALLET_CREDENTIALS)
            }.doOnComplete {
                rxBus.emitEvent(MetadataEvent::class.java, MetadataEvent.SETUP_COMPLETE)
            }
            .subscribeOn(Schedulers.io())

    private fun Completable.logOnError(tag: String): Completable =
        this.doOnError {
            crashLogger.logException(
                CustomLogMessagedException(tag, it)
            )
        }

    private fun Completable.logAndCompleteOnError(tag: String): Completable =
        this.logOnError(tag).onErrorComplete()

    fun initSettings(guid: String, sharedKey: String): Single<Settings> =
        settingsDataManager.initSettings(
            guid,
            sharedKey
        ).singleOrError()

    fun decryptAndSetupMetadata(secondPassword: String) = metadataManager.decryptAndSetupMetadata(
        secondPassword
    )

    fun warmCaches(): Completable =
        exchangeRates.prefetchCache(
            assetList = coincore.activeCryptoAssets(),
            fiatList = SUPPORTED_API_FIAT
        ).logOnError(PRICE_CACHE_PREFETCH)

    companion object {
        private const val METADATA_ERROR_MESSAGE = "metadata_init"
        private const val SIMPLE_BUY_SYNC = "simple_buy_sync"
        private const val WALLET_CREDENTIALS = "wallet_credentials"
        private const val PRICE_CACHE_PREFETCH = "price_prefetch"

        // TEMP: This will come from the /fiat BE supported assets call
        private val SUPPORTED_API_FIAT = listOf("GBP", "EUR", "USD")
    }
}