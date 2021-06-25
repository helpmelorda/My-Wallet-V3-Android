package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.junit.Test

class AssetCatalogueTest {

    private val subject = AssetCatalogueImpl()

    @Test
    fun `lowercase btc`() {
        subject.fromNetworkTicker("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BTC`() {
        subject.fromNetworkTicker("BTC") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `lowercase bch`() {
        subject.fromNetworkTicker("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BCH`() {
        subject.fromNetworkTicker("BCH") `should be` CryptoCurrency.BCH
    }

    @Test
    fun `lowercase eth`() {
        subject.fromNetworkTicker("eth") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase ETH`() {
        subject.fromNetworkTicker("ETH") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase XLM`() {
        subject.fromNetworkTicker("XLM") `should be` CryptoCurrency.XLM
    }

    @Test
    fun `lowercase xlm`() {
        subject.fromNetworkTicker("xlm") `should be` CryptoCurrency.XLM
    }

    @Test
    fun `empty should return null`() {
        subject.fromNetworkTicker("") `should be` null
    }

    @Test
    fun `not recognised should return null`() {
        subject.fromNetworkTicker("NONE") `should be` null
    }
}
