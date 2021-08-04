package piuk.blockchain.android.ui.dashboard.assetdetails

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.price.HistoricalRate
import com.blockchain.core.price.HistoricalTimeSpan
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyFrequency
import com.blockchain.nabu.models.data.RecurringBuyState
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.btc.BtcAsset
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class AssetDetailsModelTest {

    private val defaultState = AssetDetailsState(
        asset = BtcAsset(
            payloadManager = mock(),
            sendDataManager = mock(),
            feeDataManager = mock(),
            custodialManager = mock(),
            exchangeRates = mock(),
            currencyPrefs = mock(),
            labels = mock(),
            pitLinking = mock(),
            crashLogger = mock(),
            walletPreferences = mock(),
            notificationUpdater = mock(),
            identity = mock(),
            coinsWebsocket = mock(),
            features = mock()
        )
    )

    private val environmentConfig: EnvironmentConfig = mock {
        on { isRunningInDebugMode() }.thenReturn(false)
    }

    private val interactor: AssetDetailsInteractor = mock()

    private val subject = AssetDetailsModel(
        initialState = defaultState,
        mainScheduler = Schedulers.io(),
        interactor = interactor,
        environmentConfig = environmentConfig,
        crashLogger = mock()
    )

    @get:Rule
    val rx = rxInit {
        ioTrampoline()
        computationTrampoline()
    }

    @Test
    fun `load asset success`() {
        val assetDisplayMap = mapOf(
            AssetFilter.Custodial to AssetDisplayInfo(
                account = mock(),
                amount = mock(),
                pendingAmount = mock(),
                fiatValue = mock(),
                actions = emptySet()
            )
        )
        val recurringBuy = RecurringBuy(
            id = "123",
            state = RecurringBuyState.ACTIVE,
            recurringBuyFrequency = RecurringBuyFrequency.BI_WEEKLY,
            nextPaymentDate = mock(),
            paymentMethodType = PaymentMethodType.BANK_TRANSFER,
            paymentMethodId = "321",
            amount = FiatValue.zero("EUR"),
            asset = mock(),
            createDate = mock()
        )
        val recurringBuys: List<RecurringBuy> = listOf(
            recurringBuy
        )
        val expectedRecurringBuyMap = mapOf(
            "123" to recurringBuy
        )
        val price = "1000 BTC"
        val priceSeries = listOf<HistoricalRate>(mock())
        val asset: CryptoAsset = mock {
            on { asset }.thenReturn(CryptoCurrency.BTC)
        }

        val timeSpan = HistoricalTimeSpan.DAY

        whenever(interactor.loadAssetDetails(asset)).thenReturn(Single.just(assetDisplayMap))
        whenever(interactor.loadExchangeRate(asset)).thenReturn(Single.just(price))
        whenever(interactor.loadHistoricPrices(asset, timeSpan)).thenReturn(Single.just(priceSeries))
        whenever(interactor.loadRecurringBuysForAsset(asset.asset.ticker)).thenReturn(Single.just(recurringBuys))

        subject.process(LoadAsset(asset))

        subject.state.test()
            .awaitCount(7)
            .assertValueAt(0) {
                it == defaultState
            }.assertValueAt(1) {
                it == defaultState.copy(asset = asset, assetDisplayMap = mapOf())
            }.assertValueAt(2) {
                it == defaultState.copy(
                    asset = asset,
                    assetDisplayMap = mapOf(),
                    chartLoading = true
                )
            }.assertValueAt(3) {
                it == defaultState.copy(
                    asset = asset,
                    assetDisplayMap = mapOf(),
                    chartData = priceSeries,
                    chartLoading = false
                )
            }.assertValueAt(4) {
                it == defaultState.copy(
                    asset = asset,
                    assetDisplayMap = mapOf(),
                    chartData = priceSeries,
                    chartLoading = false,
                    assetFiatPrice = price
                )
            }.assertValueAt(5) {
                it == defaultState.copy(
                    asset = asset,
                    chartData = priceSeries,
                    chartLoading = false,
                    assetFiatPrice = price,
                    assetDisplayMap = assetDisplayMap
                )
            }.assertValueAt(6) {
                it == defaultState.copy(
                    asset = asset,
                    chartData = priceSeries,
                    chartLoading = false,
                    assetFiatPrice = price,
                    assetDisplayMap = assetDisplayMap,
                    recurringBuys = expectedRecurringBuyMap
                )
            }

        verify(interactor).loadAssetDetails(asset)
        verify(interactor).loadExchangeRate(asset)
        verify(interactor).loadRecurringBuysForAsset(asset.asset.ticker)
        verify(interactor).loadHistoricPrices(asset, timeSpan)

        verifyNoMoreInteractions(interactor)
    }
}