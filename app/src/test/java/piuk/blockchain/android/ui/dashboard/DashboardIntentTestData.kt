package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.FiatValue
import org.mockito.Mock
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

const val FIAT_CURRENCY = "USD"

val TEST_ASSETS = listOf(
    CryptoCurrency.BTC,
    CryptoCurrency.ETHER,
    CryptoCurrency.XLM
)

val initialBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    balance = CryptoValue.zero(CryptoCurrency.BTC),
    price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal()),
    price24h = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
    priceTrend = emptyList()
)

val initialEthState = CryptoAssetState(
    currency = CryptoCurrency.ETHER,
    balance = CryptoValue.zero(CryptoCurrency.ETHER),
    price = ExchangeRate.CryptoToFiat(CryptoCurrency.ETHER, FIAT_CURRENCY, 200.toBigDecimal()),
    price24h = ExchangeRate.CryptoToFiat(CryptoCurrency.ETHER, FIAT_CURRENCY, 100.toBigDecimal()),
    priceTrend = emptyList()
)

val initialXlmState = CryptoAssetState(
    currency = CryptoCurrency.XLM,
    balance = CryptoValue.zero(CryptoCurrency.XLM),
    price = ExchangeRate.CryptoToFiat(CryptoCurrency.XLM, FIAT_CURRENCY, 100.toBigDecimal()),
    price24h = ExchangeRate.CryptoToFiat(CryptoCurrency.XLM, FIAT_CURRENCY, 75.toBigDecimal()),
    priceTrend = emptyList()
)

val testAnnouncementCard_1 = StandardAnnouncementCard(
    name = "test_1",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testAnnouncementCard_2 = StandardAnnouncementCard(
    name = "test_2",
    dismissRule = DismissRule.CardOneTime,
    dismissEntry = mock()
)

val testBtcState = CryptoAssetState(
    currency = CryptoCurrency.BTC,
    balance = CryptoValue.fromMajor(CryptoCurrency.BTC, 10.toBigDecimal()),
    price = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal()),
    price24h = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 400.toBigDecimal()),
    priceTrend = emptyList()
)

val testFiatBalance = FiatValue.fromMajor(FIAT_CURRENCY, 1000.toBigDecimal())

@Mock
private val fiatAccount: FiatAccount = mock()
val fiatAssetState_1 = FiatAssetState()
val fiatAssetState_2 = FiatAssetState(listOf(FiatBalanceInfo(testFiatBalance, testFiatBalance, fiatAccount)))

val initialState = DashboardState(
    assets = mapOfAssets(
            CryptoCurrency.BTC to initialBtcState,
            CryptoCurrency.ETHER to initialEthState,
            CryptoCurrency.XLM to initialXlmState
        ),
    announcement = null
)
