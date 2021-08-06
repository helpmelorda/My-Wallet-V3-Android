package info.blockchain.wallet.bip44

import info.blockchain.wallet.payload.data.Derivation
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.junit.Assert
import org.junit.Test

class HDChainTest {
    private val seed = "15e23aa73d25994f1921a1256f93f72c"
    private val key = HDKeyDerivation.createMasterPrivateKey(seed.toByteArray())

    @Test
    fun getAddressAt() {
        val chain = HDChain.receiveChain(MainNetParams.get(), key)
        Assert.assertEquals(
            "1HxBEXhu5LPibpTAQ1EoNTJavDSbwajJTg",
            chain.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress
        )
    }

    @Test
    fun getSegwitAddressAt() {
        val chain = HDChain.receiveChain(MainNetParams.get(), key)
        Assert.assertEquals(
            "bc1qh8cka3lk4k74dnr7pqzyct8em57ky43a2x05lq",
            chain.getAddressAt(0, Derivation.SEGWIT_BECH32_PURPOSE).formattedAddress
        )
    }

    @Test
    fun getPath() {
        val chain = HDChain.receiveChain(MainNetParams.get(), key)
        Assert.assertEquals("M/0", chain.path)
    }
}