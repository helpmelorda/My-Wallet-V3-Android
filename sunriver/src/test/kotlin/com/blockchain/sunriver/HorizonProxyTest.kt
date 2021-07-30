package com.blockchain.sunriver

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import com.nhaarman.mockitokotlin2.internal.createInstance
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.`should throw`
import org.junit.Test
import org.koin.test.KoinTest
import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import org.stellar.sdk.KeyPair
import org.stellar.sdk.PaymentOperation
import org.stellar.sdk.Server
import org.stellar.sdk.Transaction
import org.stellar.sdk.requests.AccountsRequestBuilder
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.requests.OperationsRequestBuilder
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.requests.TransactionsRequestBuilder
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.Page
import org.stellar.sdk.responses.SubmitTransactionResponse
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.OperationResponse

class HorizonProxyTest : KoinTest {
    companion object {
        const val accountId = "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"
        val fee = 100.stroops()
        val minimumBalance = 1.lumens()
    }

    private val server: Server = mock()
    private val subject = HorizonProxy { server }

    @Test
    fun `get xlm balance`() {
        givenTheServerAccountsAre(
            createAccountsWith(
                accountId,
                createAccountResponse(
                    createBalanceWith("native", "non_valid", "0"),
                    createBalanceWith("native", null, "109969.99997")
                )
            )
        )

        val balance = subject.getBalance(accountId)

        balance `should be equal to` 109969.99997.lumens()
    }

    @Test
    fun `get xlm balance and min, account with 5x subentries`() {
        givenTheServerAccountsAre(
            createAccountsWith(
                accountId,
                createAccountResponse(
                    createBalanceWith("native", null, "100"),
                    subentryCount = 5
                )
            )
        )

        subject.getBalanceAndMin(accountId).apply {
            balance `should be equal to` 100.lumens()
            minimumBalance `should be equal to` ((2 + 5) * 0.5).lumens()
        }
    }

    @Test
    fun `get balance if account does not exist`() {
        givenTheServerAccountsAre(createAccountsWith(accountId, null))

        val balance = subject.getBalance(accountId)

        balance `should be equal to` 0.lumens()
    }

    @Test
    fun `get balance and min if account does not exist`() {
        givenTheServerAccountsAre(createAccountsWith(accountId, null))

        subject.getBalanceAndMin(accountId).apply {
            balance `should be equal to` 0.lumens()
            minimumBalance `should be equal to` 0.lumens()
        }
    }

    @Test
    fun `on any other kind of server error, bubble up exception`() {
        givenServerWillThrowErrorResponseForAccountsWith(301);

        {
            subject.getBalance(accountId)
        } `should throw` ErrorResponse::class
    }

    @Test
    fun `get xlm transaction history`() {
        val records: ArrayList<OperationResponse> = mock()

        givenTheServerOperationsWillReturnFor(accountId, records)

        val obtainedTransactionList = subject.getTransactionList(accountId)

        obtainedTransactionList `should be equal to` records
    }

    @Test
    fun `get xlm transaction history if not found`() {
        givenServerWillThrowErrorResponseForOperationsWith(404)

        val obtainedTransactionList = subject.getTransactionList(accountId)

        assert(obtainedTransactionList.isEmpty())
    }

    @Test
    fun `get xlm transaction history, on any other kind of server error, bubble up exception`() {
        givenServerWillThrowErrorResponseForOperationsWith(500);

        {
            subject.getTransactionList(accountId)
        } `should throw` ErrorResponse::class
    }

    @Test
    fun `get specific transaction by hash`() {
        val hash = "2dcb356e88d0c778a0c5ed8d33543f167994744ed0019b96553c310449133aba"
        val feeCharged = 100L

        givenServerTransactionsWillReturn(
            transactionResponse = createTransactionWith(feeCharged = feeCharged),
            withHash = hash
        )

        val transaction = subject.getTransaction(hash)

        transaction.feeCharged `should be equal to` feeCharged
    }

    @Test
    fun `accountExists - get account existence`() {
        givenTheServerAccountsAre(
            createAccountsWith(
                accountId,
                createAccountResponse(
                    createBalanceWith("native", null, "10")
                )
            )
        )

        subject.accountExists(accountId) `should be` true
    }

    @Test
    fun `accountExists - get account non-existence`() {
        givenTheServerAccountsAre(createAccountsWith(accountId, null))

        subject.accountExists(accountId) `should be` false
    }

    @Test
    fun `accountExists - on any other kind of server error, bubble up exception`() {
        givenServerWillThrowErrorResponseForAccountsWith(301);

        {
            subject.accountExists(accountId)
        } `should throw` ErrorResponse::class
    }

    @Test
    fun `can send transaction to an account that exists`() {
        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        val destinationAccountId = "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"

        givenTheServerAccountsAre(
            createAccountsWith(
                Pair(
                    destinationAccountId,
                    createAccountResponse(
                        createBalanceWith("native", null, "10000"),
                        accountId = destinationAccountId
                    )
                ),
                Pair(
                    accountId,
                    createAccountResponse(
                        createBalanceWith("native", null, "10000"),
                        accountId = accountId
                    )
                )
            )
        )

        val transactionResponse = givenASuccessfulTransactionResponse()
        whenever(
            server.submitTransaction(withAnyTransactionWith(accountId))
        ).thenReturn(
            transactionResponse
        )

        subject.update("")
        subject.sendTransaction(source, destinationAccountId, 123.4567891.lumens(), 10).apply {
            success `should be` true
            transaction `should not be` null
            transaction!!.operations.single().apply {
                this `should be instance of` PaymentOperation::class
                (this as PaymentOperation).apply {
                    destination `should be equal to` "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
                    amount `should be equal to` "123.4567891"
                }
            }
            transaction.fee `should be equal to` 100L
        }
    }

    @Test
    fun `insufficient funds that we know about before transaction send - whole balance`() {
        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        val destinationAccountId = "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"

        givenTheServerAccountsAre(
            createAccountsWith(
                Pair(
                    destinationAccountId,
                    createAccountResponse(
                        createBalanceWith("native", null, "0"),
                        accountId = destinationAccountId
                    )
                ),
                Pair(
                    accountId,
                    createAccountResponse(
                        createBalanceWith("native", null, "0"),
                        accountId = accountId
                    )
                )
            )
        )

        val transactionResponse = givenASuccessfulTransactionResponse()
        whenever(
            server.submitTransaction(withAnyTransactionWith(accountId))
        ).thenReturn(
            transactionResponse
        )

        subject.update("")
        subject.sendTransaction(
            source,
            destinationAccountId,
            500.lumens(),
            10
        ).apply {
            success `should be` false
            transaction `should be` null
            failureReason `should be` HorizonProxy.FailureReason.InsufficientFunds
            failureValue `should be equal to` 0.lumens() - minimumBalance - fee
        }
    }

    @Test
    fun `will fail when trying to send less than min`() {
        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        val destinationAccountId = "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"

        givenTheServerAccountsAre(
            createAccountsWith(
                Pair(
                    destinationAccountId,
                    createAccountResponse(
                        createBalanceWith("native", null, "0"),
                        accountId = destinationAccountId
                    )
                ),
                Pair(
                    accountId,
                    createAccountResponse(
                        createBalanceWith("native", null, "10000"),
                        accountId = accountId
                    )
                )
            )
        )

        val transactionResponse = givenASuccessfulTransactionResponse()
        whenever(
            server.submitTransaction(withAnyTransactionWith(accountId))
        ).thenReturn(
            transactionResponse
        )

        subject.update("")
        subject.sendTransaction(
            source,
            destinationAccountId,
            0.stroops(),
            10
        ).apply {
            success `should be` false
            transaction `should be` null
            failureReason `should be` HorizonProxy.FailureReason.BelowMinimumSend
            failureValue `should be equal to` 1.stroops()
        }
    }

    @Test
    fun `will fail when using BTC`() {
        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        val destinationAccountId = "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"

        subject.update("");
        {
            subject.sendTransaction(
                source,
                destinationAccountId,
                1.bitcoin(),
                10
            )
        } `should throw` IllegalArgumentException::class
    }

    @Test
    fun `will fail when destination address is invalid`() {
        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        val destinationAccountId = "invalid"

        subject.update("")
        subject.sendTransaction(
            source,
            destinationAccountId,
            1.lumens(),
            10
        ).apply {
            success `should be` false
            transaction `should be` null
            failureReason `should be` HorizonProxy.FailureReason.BadDestinationAccountId
            failureValue `should be` null
        }
    }

    private fun givenASuccessfulTransactionResponse(): SubmitTransactionResponse {
        val resultCodes: SubmitTransactionResponse.Extras.ResultCodes = mock()

        val transactionResponseExtras: SubmitTransactionResponse.Extras = mock()
        whenever(transactionResponseExtras.resultCodes).thenReturn(resultCodes)

        val transactionResponse: SubmitTransactionResponse = mock()
        whenever(transactionResponse.extras).thenReturn(transactionResponseExtras)
        whenever(transactionResponse.isSuccess).thenReturn(true)
        return transactionResponse
    }

    private fun givenTheServerAccountsAre(accounts: AccountsRequestBuilder) {
        whenever(server.accounts()).thenReturn(accounts)
    }

    private fun givenServerWillThrowErrorResponseForAccountsWith(code: Int) {
        whenever(server.accounts()).thenThrow(ErrorResponse(code, ""))
    }

    private fun givenServerWillThrowErrorResponseForOperationsWith(code: Int) {
        whenever(server.operations()).thenThrow(ErrorResponse(code, ""))
    }

    private fun createAccountsWith(id: String, response: AccountResponse?): AccountsRequestBuilder {
        return createAccountsWith(Pair(id, response))
    }

    private fun createAccountsWith(vararg accountPair: Pair<String, AccountResponse?>): AccountsRequestBuilder {
        val mock: AccountsRequestBuilder = mock()
        accountPair.forEach {
            whenever(mock.account(it.first)).thenReturn(it.second)
        }
        return mock
    }

    private fun createBalanceWith(
        assetType: String,
        assetCode: String?,
        balance: String
    ): AccountResponse.Balance {
        val mock: AccountResponse.Balance = mock()
        whenever(mock.assetType).thenReturn(assetType)
        whenever(mock.assetCode).thenReturn(assetCode)
        whenever(mock.balance).thenReturn(balance)
        return mock
    }

    private fun createAccountResponse(
        vararg balances: AccountResponse.Balance,
        subentryCount: Int = 0,
        accountId: String = ""
    ): AccountResponse {
        val mock: AccountResponse = mock()
        whenever(mock.balances).thenReturn(balances)
        whenever(mock.subentryCount).thenReturn(subentryCount)
        whenever(mock.accountId).thenReturn(accountId)
        return mock
    }

    private fun givenTheServerOperationsWillReturnFor(accountId: String, records: ArrayList<OperationResponse>) {
        val operationsRequestBuilder: OperationsRequestBuilder = mock()
        val operationResponse: Page<OperationResponse> = mock()

        whenever(server.operations()).thenReturn(operationsRequestBuilder)
        whenever(operationsRequestBuilder.order(RequestBuilder.Order.DESC)).thenReturn(operationsRequestBuilder)
        whenever(operationsRequestBuilder.limit(50)).thenReturn(operationsRequestBuilder)
        whenever(operationsRequestBuilder.forAccount(accountId)).thenReturn(operationsRequestBuilder)
        whenever(operationsRequestBuilder.execute()).thenReturn(operationResponse)
        whenever(operationResponse.records).thenReturn(records)
    }

    private fun createTransactionWith(feeCharged: Long): TransactionResponse {
        val transactionResponse: TransactionResponse = mock()
        whenever(transactionResponse.feeCharged).thenReturn(feeCharged)
        return transactionResponse
    }

    private fun givenServerTransactionsWillReturn(transactionResponse: TransactionResponse, withHash: String) {
        val transactionRequest: TransactionsRequestBuilder = mock()
        whenever(transactionRequest.transaction(withHash)).thenReturn(transactionResponse)
        whenever(server.transactions()).thenReturn(transactionRequest)
    }
}

private fun withAnyTransactionWith(sourceAccountId: String): Transaction {
    return Mockito.argThat(TransactionMatcher(sourceAccountId)) ?: createInstance()
}

private class TransactionMatcher(val sourceAccountId: String) : ArgumentMatcher<Transaction> {
    override fun matches(argument: Transaction?): Boolean {
        return argument!!.sourceAccount == sourceAccountId
    }
}