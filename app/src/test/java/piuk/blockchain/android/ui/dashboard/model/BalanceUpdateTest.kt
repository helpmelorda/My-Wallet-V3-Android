package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import piuk.blockchain.android.coincore.AccountBalance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class BalanceUpdateTest {

    @Test(expected = IllegalStateException::class)
    fun `Updating a mismatched currency throws an exception`() {

        val initialState = PortfolioState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_1
        )

        val subject = BalanceUpdate(
            CryptoCurrency.BTC,
            AccountBalance(1.bitcoinCash(), 1.bitcoinCash(), 1.bitcoinCash(), mock())
        )

        subject.reduce(initialState)
    }

    @Test
    fun `update changes effects correct asset`() {
        val initialState = PortfolioState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState,
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_1
        )

        val subject = BalanceUpdate(
            CryptoCurrency.BTC,
            AccountBalance(1.bitcoin(), 1.bitcoin(), 1.bitcoin(), mock())
        )

        val result = subject.reduce(initialState)

        assertNotEquals(result.assets, initialState.assets)
        assertNotEquals(result[CryptoCurrency.BTC], initialState[CryptoCurrency.BTC])
        assertEquals(result[CryptoCurrency.ETHER], initialState[CryptoCurrency.ETHER])
        assertEquals(result[CryptoCurrency.XLM], initialState[CryptoCurrency.XLM])

        assertEquals(result.announcement, initialState.announcement)
    }

    @Test
    fun `receiving a valid balance update clears any balance errors`() {
        val initialState = PortfolioState(
            assets = mapOfAssets(
                CryptoCurrency.BTC to initialBtcState,
                CryptoCurrency.ETHER to initialEthState.copy(hasBalanceError = true),
                CryptoCurrency.XLM to initialXlmState
            ),
            announcement = testAnnouncementCard_1
        )

        val subject = BalanceUpdate(
            CryptoCurrency.ETHER,
            AccountBalance(1.ether(), 1.ether(), 1.ether(), mock())
        )

        val result = subject.reduce(initialState)

        assertFalse(result[CryptoCurrency.ETHER].hasBalanceError)

        assertNotEquals(result.assets, initialState.assets)
        assertEquals(result[CryptoCurrency.BTC], initialState[CryptoCurrency.BTC])
        assertEquals(result[CryptoCurrency.XLM], initialState[CryptoCurrency.XLM])

        assertEquals(result.announcement, initialState.announcement)
    }
}
