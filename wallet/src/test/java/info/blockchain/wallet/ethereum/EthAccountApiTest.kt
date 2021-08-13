package info.blockchain.wallet.ethereum

import com.blockchain.nabu.util.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.internal.createInstance
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import info.blockchain.wallet.ethereum.data.EthPushTxRequest
import io.reactivex.rxjava3.core.Observable
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito

class EthAccountApiTest {
    private val ethEndpoints: EthEndpoints = mock()
    private val apiCode: ApiCode = mock()
    private val subject: EthAccountApi = EthAccountApi(ethEndpoints, apiCode)

    @Test
    fun getEthAccount() {
        val addresses = arrayListOf("firstAddress", "secondAddress")

        val expectedResponse: EthAddressResponseMap = mock()

        whenever(
            ethEndpoints.getEthAccount(addresses.joinToString(","))
        ).thenReturn(
            Observable.just(expectedResponse)
        )

        subject.getEthAddress(addresses).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == expectedResponse
            }
    }

    @Test
    fun getIfContract_returns_false() {
        val address = "address"

        whenever(
            ethEndpoints.getIfContract(address)
        ).thenReturn(
            Observable.just(HashMap(mapOf("contract" to false)))
        )

        subject.getIfContract(address).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == false
            }
    }

    @Test
    fun pushTx() {
        val rawTx = ""
        val txHash = "0xc88ac065147b34f7a4965f9b0dc539f7863468da61a73b14eb0f8f0fcbb72e5a"

        whenever(
            ethEndpoints.pushTx(withAnyRequestMatching(EthPushTxRequest(rawTx, apiCode.apiCode)))
        ).thenReturn(
            Observable.just(HashMap(mapOf("txHash" to txHash)))
        )

        subject.pushTx(rawTx).test()
            .waitForCompletionWithoutErrors().assertValue {
                it == txHash
            }
    }
}

private fun withAnyRequestMatching(request: EthPushTxRequest): EthPushTxRequest {
    return Mockito.argThat(RequestMatcher(request)) ?: createInstance()
}

private class RequestMatcher(val request: EthPushTxRequest) : ArgumentMatcher<EthPushTxRequest> {
    override fun matches(argument: EthPushTxRequest?): Boolean {
        return if (argument == null) {
            false
        } else {
            request.rawTx == argument.rawTx && request.apiCode == argument.apiCode
        }
    }
}