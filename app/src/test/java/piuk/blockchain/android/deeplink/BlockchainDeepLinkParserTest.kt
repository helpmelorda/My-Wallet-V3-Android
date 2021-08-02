package piuk.blockchain.android.deeplink

import android.net.Uri
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication

@Config(sdk = [24], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class BlockchainDeepLinkParserTest {
    private val subject = BlockchainLinkParser()

    @Test
    fun `Swap URI returns BlockchainLinkState Swap`() {
        val uri = Uri.parse(SWAP_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.Swap)
    }

    @Test
    fun `Buy URI returns BlockchainLinkState Buy`() {
        val uri = Uri.parse(BUY_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.Buy())
    }

    @Test
    fun `TwoFa URI returns BlockchainLinkState TwoFa`() {
        val uri = Uri.parse(TWOFA_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.TwoFa)
    }

    @Test
    fun `Empty URI returns NoUri`() {
        val uri = Uri.parse("")
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.NoUri)
    }

    companion object {
        private const val SWAP_URL =
            "blockchain://flow/swap"
        private const val BUY_URL =
            "blockchain://flow/buy"
        private const val TWOFA_URL =
            "blockchain://flow/twofa"
    }
}