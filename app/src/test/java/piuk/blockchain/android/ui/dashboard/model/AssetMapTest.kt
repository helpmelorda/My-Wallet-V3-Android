package piuk.blockchain.android.ui.dashboard.model

import com.blockchain.core.price.ExchangeRate
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import org.junit.Test
import piuk.blockchain.android.coincore.AccountBalance
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame

class AssetMapTest {

    private val subject = AssetMap(
        map = mapOfAssets(
            CryptoCurrency.BTC to initialBtcState,
            CryptoCurrency.ETHER to initialEthState,
            CryptoCurrency.XLM to initialXlmState
        )
    )

    @Test(expected = IllegalArgumentException::class)
    fun `Exception thrown if unknown asset requested from get()`() {
        val invalidAsset: AssetInfo = mock()

        subject[invalidAsset]
    }

    @Test
    fun `copy with patchAsset works as expected`() {
        val newAsset = CryptoAssetState(
            currency = CryptoCurrency.BTC,
            accountBalance = AccountBalance(
                20.bitcoin(), 20.bitcoin(), 20.bitcoin(),
                ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, FIAT_CURRENCY, 300.toBigDecimal())
            ),
            prices24HrWithDelta = mock(),
            priceTrend = emptyList()
        )

        val copy = subject.copy(patchAsset = newAsset)

        assertNotEquals(copy[CryptoCurrency.BTC], subject[CryptoCurrency.BTC])
        assertEquals(copy[CryptoCurrency.BTC], newAsset)
        assertEquals(copy[CryptoCurrency.ETHER], subject[CryptoCurrency.ETHER])
        assertEquals(copy[CryptoCurrency.XLM], subject[CryptoCurrency.XLM])
    }

    @Test
    fun `copy with patchBalance works as expected`() {
        val newBalance = 20.ether()
        val newAccountBalance = AccountBalance(newBalance, newBalance, newBalance, mock())

        val copy = subject.copy(patchBalance = newAccountBalance)

        assertEquals(copy[CryptoCurrency.BTC], subject[CryptoCurrency.BTC])
        assertNotEquals(copy[CryptoCurrency.ETHER], subject[CryptoCurrency.ETHER])
        assertEquals(copy[CryptoCurrency.ETHER].accountBalance, newAccountBalance)
        assertEquals(copy[CryptoCurrency.XLM], subject[CryptoCurrency.XLM])
    }

    @Test
    fun `reset() replaces all assets`() {
        val result = subject.reset()

        assertEquals(result.size, subject.size)
        subject.keys.forEach {
            assertNotSame(result[it], subject[it])
        }
    }
}
