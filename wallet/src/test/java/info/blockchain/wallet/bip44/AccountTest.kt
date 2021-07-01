package info.blockchain.wallet.bip44

import info.blockchain.wallet.payload.data.Derivation
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.params.MainNetParams
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountTest {
    var seed = "15e23aa73d25994f1921a1256f93f72c"
    var xpub = "xpub6CbTPgFYkRqMQZiX2WYEiVHWGJUjAsZAvSvMq3z52KczYQrZPQ9" +
        "DjKwHQBmAMJVY3kLeBQ4T818MBf2cTiGkJSkmS8CDT1Wp7Dw4vFMygEV"
    var xpriv = "xprv9v8qUhWNujRdAYSQHLkaX6wZbYfAv6VLZUnWAR5C5UK6ZE4KKj" +
        "cZQMWBECoGcrGJMiPf3KDATPyqa8zurUu8T5Cfuz9BNXizu2AtK84MecB"
    var key: DeterministicKey? = HDKeyDerivation.createMasterPrivateKey(seed.toByteArray())

    @Test
    fun xpubstr() {
        val account = HDAccount(MainNetParams.get(), xpub)
        assertEquals(xpub, account.xpub)
    }

    @Test
    fun xprvstr() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        assertEquals(xpriv, account.xPriv)
    }

    @Test
    fun getId() {
        val account1 = HDAccount(MainNetParams.get(), xpub, 1)
        assertEquals(xpub, account1.xpub)
        assertEquals(1, account1.id.toLong())

        val account2 = HDAccount(MainNetParams.get(), xpub)
        assertEquals(xpub, account2.xpub)
        assertEquals(0, account2.id.toLong())
    }

    @Test
    fun getReceive() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        val address = account.receive.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress

        assertEquals("M/0H/0", account.receive.path)
        assertEquals("1GfNtDKUu9KZt8ae7c9UM6NUD1uViZcsEA", address)
    }

    @Test
    fun getChangeLegacy() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        val address = account.change.getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress

        assertEquals("M/0H/1", account.change.path)
        assertEquals("12boKefnALjsXoQXyHg79aU7qSAFfg5Nze", address)
    }

    @Test
    fun getChangeSegwit() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        val address = account.change.getAddressAt(0, Derivation.SEGWIT_BECH32_PURPOSE).formattedAddress

        assertEquals("M/0H/1", account.change.path)
        assertEquals("bc1qzxxwlrs88fmjr79hk8vavc2e68ml0q22n9enkm", address)
    }

    @Test
    fun getAddressFromChainLegacy() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        val address = account.getChain(1)
            .getAddressAt(0, Derivation.LEGACY_PURPOSE).formattedAddress

        assertEquals("M/0H/1", account.getChain(1).path)
        assertEquals("12boKefnALjsXoQXyHg79aU7qSAFfg5Nze", address)
    }

    @Test
    fun getAddressFromChainSegwit() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        val address = account.getChain(1)
            .getAddressAt(0, Derivation.SEGWIT_BECH32_PURPOSE).formattedAddress

        assertEquals("M/0H/1", account.getChain(1).path)
        assertEquals("bc1qzxxwlrs88fmjr79hk8vavc2e68ml0q22n9enkm", address)
    }

    @Test
    fun getPath() {
        val account = HDAccount(MainNetParams.get(), key, 0)
        assertEquals("M/0H", account.path)
    }
}
