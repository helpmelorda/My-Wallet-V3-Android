package piuk.blockchain.android.kyc

import com.nhaarman.mockitokotlin2.mock
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.data.WalletOptions
import io.reactivex.rxjava3.core.Observable

import org.junit.Test

class KycServerSideFeatureFlagTest {

    @Test
    fun `if homebrew is set to true, the feature flag is on`() {
        mock<WalletApi> {
            on { walletOptions }.thenReturn(Observable.just(
                WalletOptions().apply {
                    androidFlags["homebrew"] = true
                }
            ))
        }.let { KycServerSideFeatureFlag(it) }
            .apply {
                enabled.test()
                    .assertValue(true)
            }
    }

    @Test
    fun `if homebrew is set to false, the feature flag is off`() {
        mock<WalletApi> {
            on { walletOptions }.thenReturn(Observable.just(
                WalletOptions().apply {
                    androidFlags["homebrew"] = false
                }
            ))
        }.let { KycServerSideFeatureFlag(it) }
            .apply {
                enabled.test()
                    .assertValue(false)
            }
    }

    @Test
    fun `if homebrew is missing, the feature flag is off`() {
        mock<WalletApi> {
            on { walletOptions }.thenReturn(Observable.just(
                WalletOptions()
            ))
        }.let { KycServerSideFeatureFlag(it) }
            .apply {
                enabled.test()
                    .assertValue(false)
            }
    }
}