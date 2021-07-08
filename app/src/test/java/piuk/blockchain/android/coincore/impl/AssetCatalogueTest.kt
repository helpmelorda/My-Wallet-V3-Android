package piuk.blockchain.android.coincore.impl

import com.blockchain.android.testutils.rxInit
import com.blockchain.remoteconfig.RemoteConfig
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import org.amshove.kluent.`should be`
import org.amshove.kluent.itReturns
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

class AssetCatalogueTest {

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val remoteConfig: RemoteConfig = mock {
        on { getRawJson(AssetCatalogueImpl.CUSTODIAL_ONLY_TOKENS) } itReturns Single.just(DYNAMIC_ENABLED_JSON)
    }

    private val subject = AssetCatalogueImpl(remoteConfig)

    @Before
    fun init() {
        subject.init().emptySubscribe()
    }

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

    companion object {
        private const val DYNAMIC_ENABLED_JSON = "[\"DOT\", \"ALGO\", \"DOGE\", \"CLOUT\", \"LTC\", \"ETC\"]"
    }
}
