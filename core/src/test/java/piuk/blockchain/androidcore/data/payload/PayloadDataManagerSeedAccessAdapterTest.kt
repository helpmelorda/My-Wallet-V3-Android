package piuk.blockchain.androidcore.data.payload

import com.blockchain.wallet.SeedAccessWithoutPrompt
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import info.blockchain.wallet.payload.data.WalletBody
import info.blockchain.wallet.payload.data.Wallet

import org.amshove.kluent.`should be`
import org.junit.Test

class PayloadDataManagerSeedAccessAdapterTest {

    @Test
    fun `extracts seed from the first HDWallet`() {
        val theSeed = byteArrayOf(1, 2, 3)
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(
            givenADecodedPayload(theSeed)
        )

        seedAccess.seed
            .test()
            .values()
            .single()
            .apply {
                hdSeed `should be` theSeed
            }
    }

    @Test
    fun `if the HD wallet throws HD Exception, returns empty`() {
        val theSeed = byteArrayOf(1, 2, 3)
        val hdWallet = mock<WalletBody> {
            on { hdSeed }.thenReturn(theSeed)
        }
        val wallet = mock<Wallet> {
            on { walletBodies }.thenReturn(listOf(hdWallet))
        }
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet }.thenReturn(wallet)
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(payloadDataManager)
        seedAccess.seed
            .test()
            .assertComplete()
            .assertValueCount(0)
    }

    @Test
    fun `if the list is null, returns empty`() {
        val wallet = mock<Wallet> {
            on { walletBody }.thenReturn(null)
        }
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet }.thenReturn(wallet)
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(payloadDataManager)
        seedAccess.seed
            .test()
            .assertComplete()
            .assertValueCount(0)
    }

    @Test
    fun `if the wallet is null, returns empty`() {
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet }.thenReturn(null)
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(payloadDataManager)
        seedAccess.seed
            .test()
            .assertComplete()
            .assertValueCount(0)
    }

    @Test
    fun `extracts seed from the first HDWallet without decrypting - when already decoded`() {
        val theSeed = byteArrayOf(1, 2, 3)
        val payloadDataManager = givenADecodedPayload(theSeed)
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(
            payloadDataManager
        )
        seedAccess.seed("PASSWORD").test().values().single()
            .apply {
                hdSeed `should be` theSeed
            }
        verify(payloadDataManager, never()).decryptHDWallet(any())
    }

    @Test
    fun `decrypts if required`() {
        val payloadDataManager = mock<PayloadDataManager> {
            on { this.wallet }.thenReturn(null)
        }
        val seedAccess: SeedAccessWithoutPrompt = PayloadDataManagerSeedAccessAdapter(
            payloadDataManager
        )
        seedAccess.seed("PASSWORD").test()
        verify(payloadDataManager).decryptHDWallet("PASSWORD")
    }
}

private fun givenADecodedPayload(
    theSeed: ByteArray
): PayloadDataManager {
    val hdwallet = mock<WalletBody> {
        on { hdSeed }.thenReturn(theSeed)
    }
    val wallet = mock<Wallet> {
        on { walletBody }.thenReturn(hdwallet)
    }
    return mock {
        on { this.wallet }.thenReturn(wallet)
    }
}
