package piuk.blockchain.android.coincore.impl

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Completable
import org.amshove.kluent.`should be`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.loader.AssetCatalogueImpl
import piuk.blockchain.android.coincore.loader.AssetRemoteFeatureLookup
import piuk.blockchain.android.coincore.loader.RemoteFeature
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe

class AssetCatalogueTest {

    @get:Rule
    val rx = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val featureConfig: AssetRemoteFeatureLookup = mock {
        on { init(any()) }.thenReturn(Completable.complete())
        on { featuresFor(anyOrNull()) }.thenReturn(setOf(RemoteFeature.Balance))
    }

    private val subject = AssetCatalogueImpl(
        featureConfig = featureConfig
    )

    @Before
    fun before() {
        subject.initialise(
            setOf(
                CryptoCurrency.BTC,
                CryptoCurrency.BCH,
                CryptoCurrency.ETHER,
                CryptoCurrency.XLM
            )
        ).emptySubscribe()
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
}
