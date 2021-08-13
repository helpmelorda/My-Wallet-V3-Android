package info.blockchain.wallet.api

import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.FeeLimits
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Test

class FeeApiTest {
    private val feeEndpoints: FeeEndpoints = mock()
    private val feeOptions: FeeOptions = mock()
    private val subject: FeeApi = FeeApi(feeEndpoints)

    @Before
    fun setup() {
        withDefaultFeeOptions()
    }

    @Test
    fun `get btc fee options`() {
        whenever(
            feeEndpoints.btcFeeOptions
        ).thenReturn(
            Observable.just(feeOptions)
        )

        subject.btcFeeOptions.test()
            .waitForCompletionWithoutErrors().assertValue {
                it.priorityFee == FEE_PRIORITY
                it.regularFee == FEE_REGULAR
                it.limits!!.max == LIMIT_MAX
                it.limits!!.min == LIMIT_MIN
            }
    }

    @Test
    fun `get eth fee options`() {
        whenever(
            feeEndpoints.ethFeeOptions
        ).thenReturn(
            Observable.just(feeOptions)
        )

        subject.ethFeeOptions.test()
            .waitForCompletionWithoutErrors().assertValue {
                it.priorityFee == FEE_PRIORITY
                it.regularFee == FEE_REGULAR
                it.limits!!.max == LIMIT_MAX
                it.limits!!.min == LIMIT_MIN
                it.gasLimit == GAS_LIMIT
            }
    }

    private fun withDefaultFeeOptions() {
        whenever(feeOptions.priorityFee).thenReturn(FEE_PRIORITY)
        whenever(feeOptions.regularFee).thenReturn(FEE_REGULAR)
        whenever(feeOptions.limits).thenReturn(FeeLimits(LIMIT_MIN, LIMIT_MAX))
        whenever(feeOptions.gasLimit).thenReturn(GAS_LIMIT)
    }

    companion object {
        private const val GAS_LIMIT = 3000L
        private const val LIMIT_MIN = 3000L
        private const val LIMIT_MAX = 5000L
        private const val FEE_PRIORITY = 5L
        private const val FEE_REGULAR = 2L
    }
}