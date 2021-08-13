package piuk.blockchain.android.coincore.fiat

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.core.custodial.TradingBalanceDataManager
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount

class FiatAssetTransferTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val labels: DefaultLabels = mock()
    private val exchangeRateDataManager: ExchangeRatesDataManager = mock()
    private val tradingBalanceDataManager: TradingBalanceDataManager = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val currencyPrefs: CurrencyPrefs = mock()

    private val subject = FiatAsset(
        labels,
        exchangeRateDataManager,
        tradingBalanceDataManager,
        custodialWalletManager,
        currencyPrefs
    )

    @Test
    fun transferListForCustodialSource() {
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn(SELECTED_FIAT)
        whenever(custodialWalletManager.getSupportedFundsFiats(any()))
            .thenReturn(Single.just(FIAT_ACCOUNT_LIST))

        whenever(labels.getDefaultCustodialFiatWalletLabel(any())).thenReturn(DEFAULT_LABEL)

        val sourceAccount: CustodialTradingAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue { list ->
                list.isEmpty()
            }
    }

    @Test
    fun transferListForInterestSource() {
        val sourceAccount: CryptoInterestAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }

    @Test
    fun transferListForNonCustodialSource() {
        val sourceAccount: CryptoNonCustodialAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }

    @Test
    fun transferListForFiatSource() {
        val sourceAccount: FiatCustodialAccount = mock()

        subject.transactionTargets(sourceAccount)
            .test()
            .assertNoErrors()
            .assertResult(emptyList())
    }

    companion object {
        private const val DEFAULT_LABEL = "label"
        private const val SELECTED_FIAT = "USD"
        private val FIAT_ACCOUNT_LIST = listOf("USD", "GBP")
    }
}
