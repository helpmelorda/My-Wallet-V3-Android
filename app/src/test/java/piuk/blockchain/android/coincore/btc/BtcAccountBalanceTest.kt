package piuk.blockchain.android.coincore.btc

import com.blockchain.core.price.ExchangeRate
import com.blockchain.preferences.WalletStatus
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import junit.framework.Assert.assertFalse

import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.coincore.impl.AccountRefreshTrigger
import piuk.blockchain.android.coincore.testutil.CoincoreTestBase
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager

class BtcAccountBalanceTest : CoincoreTestBase() {

    private val payloadDataManager: PayloadDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val feeDataManager: FeeDataManager = mock()
    private val walletPrefs: WalletStatus = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val refreshTrigger: AccountRefreshTrigger = mock()

    private val xpubs = (XPubs(listOf(XPub(ACCOUNT_XPUB, XPub.Format.LEGACY))))
    private val jsonAccount: Account = mock {
        on { isArchived }.thenReturn(false)
        on { xpubs }.thenReturn(xpubs)
    }

    private val subject =
        BtcCryptoWalletAccount(
            payloadManager = payloadDataManager,
            hdAccountIndex = -1,
            sendDataManager = sendDataManager,
            feeDataManager = feeDataManager,
            exchangeRates = exchangeRates,
            internalAccount = jsonAccount,
            isHDAccount = true,
            walletPreferences = walletPrefs,
            custodialWalletManager = custodialWalletManager,
            refreshTrigger = refreshTrigger,
            identity = mock()
        )

    @Before
    fun setup() {
        initMocks()
        whenever(exchangeRates.cryptoToUserFiatRate(CryptoCurrency.BTC))
            .thenReturn(Observable.just(BTC_TO_USER_RATE))
    }

    @Test
    fun `non zero balance calculated correctly`() {
        val btcBalance = 100.bitcoin()

        whenever(payloadDataManager.getAddressBalanceRefresh(eq(xpubs), any()))
            .thenReturn(Single.just(btcBalance))

        subject.balance
            .test()
            .assertValue {
                it.total == btcBalance &&
                    it.actionable == btcBalance &&
                    it.pending.isZero &&
                    it.exchangeRate == BTC_TO_USER_RATE
            }

        assert(subject.isFunded)
    }

    @Test
    fun `zero balance calculated correctly`() {
        val btcBalance = 0.bitcoin()

        whenever(payloadDataManager.getAddressBalanceRefresh(eq(xpubs), any()))
            .thenReturn(Single.just(btcBalance))

        subject.balance
            .test()
            .assertValue {
                it.total.isZero &&
                it.actionable.isZero &&
                it.pending.isZero &&
                it.exchangeRate == BTC_TO_USER_RATE
            }

        assertFalse(subject.isFunded)
    }

    companion object {
        private const val ACCOUNT_XPUB = "1234jfwepsdfapsksefksdwperoun894y98hefjbnakscdfoiw4rnwef"

        private val BTC_TO_USER_RATE = ExchangeRate.CryptoToFiat(
            from = CryptoCurrency.BTC,
            to = TEST_USER_FIAT,
            rate = 38000.toBigDecimal()
        )
    }
}
