package com.blockchain.sunriver

import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.ManageDataOperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.util.Locale

class HorizonOperationMappingTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `map response rejects unsupported types`() {
        val unsupportedResponse: ManageDataOperationResponse = mock();
        {
            mapOperationResponse(unsupportedResponse, "", givenHorizonProxy(100))
        } `should throw` IllegalArgumentException::class
    }

    @Test
    fun `map payment operation where account is the receiver`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<PaymentOperationResponse> {
            on { from }.thenReturn(otherAccount)
            on { to }.thenReturn(myAccount)
            on { transactionHash }.thenReturn("ABCD")
            on { createdAt }.thenReturn("TIME")
            on { amount }.thenReturn(50.lumens().toStringWithoutSymbol())
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should be equal to` "ABCD"
                timeStamp `should be equal to` "TIME"
                fee `should be equal to` 100.stroops()
                from.accountId `should be equal to` otherAccount
                to.accountId `should be equal to` myAccount
                value `should be equal to` 50.lumens()
            }
    }

    @Test
    fun `map payment operation where account is the sender`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<PaymentOperationResponse> {
            on { from }.thenReturn(myAccount)
            on { to }.thenReturn(otherAccount)
            on { transactionHash }.thenReturn("ABCD")
            on { createdAt }.thenReturn("TIME")
            on { amount }.thenReturn(50.lumens().toStringWithoutSymbol())
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should be equal to` "ABCD"
                timeStamp `should be equal to` "TIME"
                fee `should be equal to` 100.stroops()
                from.accountId `should be equal to` myAccount
                to.accountId `should be equal to` otherAccount
                value `should be equal to` (-50).lumens()
            }
    }

    @Test
    fun `map create operation where account is the receiver`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<CreateAccountOperationResponse> {
            on { funder }.thenReturn(otherAccount)
            on { account }.thenReturn(myAccount)
            on { transactionHash }.thenReturn("ABCD")
            on { createdAt }.thenReturn("TIME")
            on { startingBalance }.thenReturn(100.lumens().toStringWithoutSymbol())
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should be equal to` "ABCD"
                timeStamp `should be equal to` "TIME"
                fee `should be equal to` 100.stroops()
                from.accountId `should be equal to` otherAccount
                to.accountId `should be equal to` myAccount
                value `should be equal to` 100.lumens()
            }
    }

    @Test
    fun `map create operation where account is the sender`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<CreateAccountOperationResponse> {
            on { funder }.thenReturn(myAccount)
            on { account }.thenReturn(otherAccount)
            on { transactionHash }.thenReturn("ABCD")
            on { createdAt }.thenReturn("TIME")
            on { startingBalance }.thenReturn(100.lumens().toStringWithoutSymbol())
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should be equal to` "ABCD"
                timeStamp `should be equal to` "TIME"
                fee `should be equal to` 100.stroops()
                from.accountId `should be equal to` myAccount
                to.accountId `should be equal to` otherAccount
                value `should be equal to` (-100).lumens()
            }
    }

    private fun givenHorizonProxy(fee: Long): HorizonProxy {
        val mockTx: TransactionResponse = mock {
            on { feeCharged }.thenReturn(fee)
        }
        return mock {
            on { getTransaction(any()) }.thenReturn(mockTx)
        }
    }
}
