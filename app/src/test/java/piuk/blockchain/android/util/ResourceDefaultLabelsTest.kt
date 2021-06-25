package piuk.blockchain.android.util

import android.content.res.Resources
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockito_kotlin.mock
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.resources.AssetResources

class ResourceDefaultLabelsTest {

    private val resources: Resources = mock {
        on { getString(R.string.default_crypto_non_custodial_wallet_label) } `it returns` "Private Key"
    }

    private val assetResources: AssetResources = mock()

    private val defaultLabels: DefaultLabels =
        ResourceDefaultLabels(resources, assetResources)

    @Test
    fun `btc default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should equal` "Private Key"
    }

    @Test
    fun `ether default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should equal` "Private Key"
    }

    @Test
    fun `bch default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should equal` "Private Key"
    }

    @Test
    fun `xlm default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel() `should equal` "Private Key"
    }
}
