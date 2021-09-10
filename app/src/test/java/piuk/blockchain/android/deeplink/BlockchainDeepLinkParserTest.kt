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
    private val subject = BlockchainDeepLinkParser()

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
    fun `Verify Email URI returns BlockchainLinkState VerifyEmail`() {
        val uri = Uri.parse(VERIFY_EMAIL_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.VerifyEmail)
    }

    @Test
    fun `Fingerprint URI returns BlockchainLinkState SetupFingerprint`() {
        val uri = Uri.parse(FINGERPRINT_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.SetupFingerprint)
    }

    @Test
    fun `Interest URI returns BlockchainLinkState Interest`() {
        val uri = Uri.parse(INTEREST_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.Interest)
    }

    @Test
    fun `Receive URI returns BlockchainLinkState Receive`() {
        val uri = Uri.parse(RECEIVE_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.Receive)
    }

    @Test
    fun `Send URI returns BlockchainLinkState SetupFingerprint`() {
        val uri = Uri.parse(SEND_URL)
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.Send)
    }

    @Test
    fun `Empty URI returns NoUri`() {
        val uri = Uri.parse("")
        Assert.assertEquals(subject.mapUri(uri), BlockchainLinkState.NoUri)
    }

    companion object {
        private const val SWAP_URL = "https://login-staging.blockchain.com/#/open/swap"
        private const val BUY_URL = "https://login-staging.blockchain.com/#/open/buy"
        private const val TWOFA_URL = "https://login-staging.blockchain.com/#/open/twofa"
        private const val FINGERPRINT_URL = "https://login-staging.blockchain.com/#/open/setupfingerprint"
        private const val VERIFY_EMAIL_URL = "https://login-staging.blockchain.com/#/open/verifyemail"
        private const val INTEREST_URL = "https://login-staging.blockchain.com/#/open/interest"
        private const val RECEIVE_URL = "https://login-staging.blockchain.com/#/open/receive"
        private const val SEND_URL = "https://login-staging.blockchain.com/#/open/send"
    }
}