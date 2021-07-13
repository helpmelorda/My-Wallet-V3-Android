package com.blockchain.sunriver

import com.blockchain.fees.FeeType
import com.blockchain.logging.CustomEventBuilder
import com.blockchain.logging.EventLogger
import com.blockchain.logging.LastTxUpdater
import com.blockchain.sunriver.datamanager.XlmAccount
import com.blockchain.sunriver.datamanager.XlmMetaData
import com.blockchain.sunriver.datamanager.XlmMetaDataInitializer
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.testutils.lumens
import com.blockchain.testutils.rxInit
import com.blockchain.testutils.stroops
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.balance.CryptoValue
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test
import org.spongycastle.asn1.cmc.CMCStatus.success
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Transaction
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import org.stellar.sdk.responses.operations.SetOptionsOperationResponse

class XlmDataManagerTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `getBalance - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            getBalance()
        }
    }

    @Test
    fun `getBalance with reference - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            getBalance(XlmAccountReference("", "ANY"))
        }
    }

    @Test
    fun `balanceOf with reference - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            getBalance(XlmAccountReference("", "ANY"))
        }
    }

    @Test
    fun `get balance for an account reference`() {
        givenXlmDataManager(
            givenBalances("ANY" to 123.lumens())
        )
            .getBalance(XlmAccountReference("", "ANY"))
            .testSingle() `should be equal to` 123.lumens()
    }

    @Test
    fun `balanceOf an account reference`() {
        givenXlmDataManager(
            givenBalances("ANY" to 456.lumens())
        )
            .getBalance(XlmAccountReference("", "ANY"))
            .testSingle() `should be equal to` 456.lumens()
    }

    @Test
    fun `get default account balance`() {
        givenXlmDataManager(
            givenBalances("GABC1234" to 456.lumens()),
            givenMetaDataMaybe(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GABC1234",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .getBalance()
            .testSingle() `should be equal to` 456.lumens()
    }

    @Test
    fun `get default account max spendable`() {
        givenXlmDataManager(
            givenBalancesAndMinimums(
                "GABC1234" to BalanceAndMin(
                    balance = 456.lumens(),
                    minimumBalance = 4.lumens()
                )
            ),
            givenMetaDataMaybe(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GABC1234",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            feesFetcher = givenXlmFees(450.stroops())
        )
            .getMaxSpendableAfterFees(FeeType.Regular)
            .testSingle() `should be equal to` 456.lumens() - 4.lumens() - 450.stroops()
    }

    @Test
    fun `get default account balance and min`() {
        givenXlmDataManager(
            givenBalancesAndMinimums(
                "GABC1234" to BalanceAndMin(
                    balance = 456.lumens(),
                    minimumBalance = 4.lumens()
                )
            ),
            givenMetaDataMaybe(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GABC1234",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .getBalanceAndMin()
            .testSingle().apply {
                minimumBalance `should be equal to` 4.lumens()
                balance `should be equal to` 456.lumens()
            }
    }

    @Test
    fun `get default account balance without metadata`() {
        givenXlmDataManager(
            givenBalances("GABC1234" to 456.lumens()),
            givenNoMetaData()
        )
            .getBalance()
            .testSingle() `should be equal to` 0.lumens()
    }

    @Test
    fun `get default account 0`() {
        givenXlmDataManager(
            metaDataInitializer = givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .defaultAccount()
            .testSingle()
            .apply {
                label `should be equal to` "Account #1"
                accountId `should be equal to` "ADDRESS1"
            }
    }

    @Test
    fun `get maybe default account 0`() {
        givenXlmDataManager(
            metaDataInitializer = givenMetaDataMaybe(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .maybeDefaultAccount()
            .toSingle()
            .testSingle()
            .apply {
                label `should be equal to` "Account #1"
                accountId `should be equal to` "ADDRESS1"
            }
    }

    @Test
    fun `get default account 1`() {
        givenXlmDataManager(
            metaDataInitializer = givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 1,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .defaultAccount()
            .testSingle()
            .apply {
                label `should be equal to` "Account #2"
                accountId `should be equal to` "ADDRESS2"
            }
    }

    @Test
    fun `defaultAccount and defaultAccountReference are equal`() {
        val dataManager = givenXlmDataManager(
            metaDataInitializer = givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 1,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
        val accountReference: XlmAccountReference = dataManager.defaultAccountReference().testSingle()
        val accountReferenceXlm: XlmAccountReference = dataManager.defaultAccount().testSingle()
        accountReference `should be equal to` accountReferenceXlm
    }

    @Test
    fun `get maybe default account 1`() {
        givenXlmDataManager(
            metaDataInitializer = givenMetaDataMaybe(
                XlmMetaData(
                    defaultAccountIndex = 1,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .maybeDefaultAccount()
            .toSingle()
            .testSingle()
            .apply {
                label `should be equal to` "Account #2"
                accountId `should be equal to` "ADDRESS2"
            }
    }

    @Test
    fun `get default account 1 - balance`() {
        givenXlmDataManager(
            givenBalances(
                "ADDRESS1" to 10.lumens(),
                "ADDRESS2" to 20.lumens()
            ),
            givenMetaDataMaybe(
                XlmMetaData(
                    defaultAccountIndex = 1,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .getBalance()
            .testSingle() `should be equal to` 20.lumens()
    }

    @Test
    fun `get either balance by address`() {
        val xlmDataManager = givenXlmDataManager(
            givenBalances(
                "ADDRESS1" to 10.lumens(),
                "ADDRESS2" to 20.lumens()
            ),
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 1,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "ADDRESS1",
                            label = "Account #1",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "ADDRESS2",
                            label = "Account #2",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
        xlmDataManager
            .getBalance(XlmAccountReference("", "ADDRESS1"))
            .testSingle() `should be equal to` 10.lumens()
        xlmDataManager
            .getBalance(XlmAccountReference("", "ADDRESS2"))
            .testSingle() `should be equal to` 20.lumens()
    }
}

class XlmDataManagerTransactionListTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `getTransactionList - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            getTransactionList()
        }
    }

    @Test
    fun `getTransactionList with reference - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            getTransactionList(XlmAccountReference("", "ANY"))
        }
    }

    @Test
    fun `get transaction list from default account`() {
        givenXlmDataManager(
            givenTransactions(
                1,
                "GC24LNYWXIYYB6OGCMAZZ5RX6WPI2F74ZV7HNBV4ADALLXJRT7ZTLHP2" to getResponseList()
            ),
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GC24LNYWXIYYB6OGCMAZZ5RX6WPI2F74ZV7HNBV4ADALLXJRT7ZTLHP2",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            )
        )
            .getTransactionList()
            .testSingle() `should be equal to` getXlmList()
    }

    @Test
    fun `get transactions`() {
        givenXlmDataManager(
            givenTransactions(
                1,
                "GC24LNYWXIYYB6OGCMAZZ5RX6WPI2F74ZV7HNBV4ADALLXJRT7ZTLHP2" to getResponseList()
            )
        )
            .getTransactionList(
                XlmAccountReference("", "GC24LNYWXIYYB6OGCMAZZ5RX6WPI2F74ZV7HNBV4ADALLXJRT7ZTLHP2")
            )
            .testSingle() `should be equal to` getXlmList()
    }

    private fun getXlmList(): List<XlmTransaction> = listOf(
        XlmTransaction(
            timeStamp = "createdAt",
            value = 10000.lumens(),
            fee = 1.stroops(),
            hash = "transactionHash",
            memo = Memo.None,
            to = HorizonKeyPair.Public("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"),
            from = HorizonKeyPair.Public("GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR")
        ),
        XlmTransaction(
            timeStamp = "createdAt",
            value = (-100).lumens(),
            fee = 1.stroops(),
            hash = "transactionHash",
            memo = Memo.None,
            to = HorizonKeyPair.Public("GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"),
            from = HorizonKeyPair.Public("GC24LNYWXIYYB6OGCMAZZ5RX6WPI2F74ZV7HNBV4ADALLXJRT7ZTLHP2")
        )
    )

    private fun getResponseList(): List<OperationResponse> {
        val mockIgnored: SetOptionsOperationResponse = mock()
        val mockCreate: CreateAccountOperationResponse = mock {
            on { createdAt }.thenReturn("createdAt")
            on { startingBalance }.thenReturn("10000")
            on { transactionHash }.thenReturn("transactionHash")
            on { account }.thenReturn(
                "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
            )
            on { funder }.thenReturn(
                "GAIH3ULLFQ4DGSECF2AR555KZ4KNDGEKN4AFI4SU2M7B43MGK3QJZNSR"
            )
        }
        val mockPayment: PaymentOperationResponse = mock {
            on { createdAt }.thenReturn("createdAt")
            on { amount }.thenReturn("100")
            on { transactionHash }.thenReturn("transactionHash")
            on { to }.thenReturn(
                "GBAHSNSG37BOGBS4GXUPMHZWJQ22WIOJQYORRBHTABMMU6SGSKDEAOPT"
            )
            on { from }.thenReturn(
                "GC24LNYWXIYYB6OGCMAZZ5RX6WPI2F74ZV7HNBV4ADALLXJRT7ZTLHP2"
            )
            on { type }.thenReturn("payment")
        }

        return listOf(mockIgnored, mockCreate, mockPayment)
    }
}

class XlmDataManagerSendTransactionTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `dryRunSendFunds with reference - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            dryRunSendFunds(
                SendDetails(
                    XlmAccountReference("", "ANY"),
                    100.lumens(),
                    "ANY",
                    1.stroops()
                )
            )
        }
    }

    @Test
    fun `sendFunds with reference - there should be no interactions before subscribe`() {
        verifyNoInteractionsBeforeSubscribe {
            sendFunds(
                SendDetails(
                    XlmAccountReference("", "ANY"),
                    100.lumens(),
                    "ANY",
                    1.stroops()
                )
            )
        }
    }

    @Test
    fun `can send`() {
        val eventLogger: EventLogger = mock()
        val lastTxUpdater: LastTxUpdater = givenLastTxUpdater()
        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(127, 128.toByte(), 255.toByte()))
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(
                    source = keyPairEq(
                        KeyPair.fromSecretSeed(
                            "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
                        )
                    ),
                    destinationAccountId = eq(
                        "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3"
                    ),
                    amount = eq(199.456.lumens()),
                    memo = eq(org.stellar.sdk.Memo.none()),
                    timeout = any(),
                    perOperationFee = eq(256.stroops())
                )
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            ),
            feesFetcher = givenXlmFees(256.stroops()),
            eventLogger = eventLogger,
            lastTxUpdater = lastTxUpdater
        ).sendFunds(
            SendDetails(
                XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
                199.456.lumens(),
                "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
                256.stroops()
            )
        )
            .test()
            .assertNoErrors()
            .assertComplete()
            .values().single().hash `should be equal to` "7F80FF"

        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
        verify(eventLogger).logEvent(any())
        verify(lastTxUpdater).updateLastTxTime()
    }

    @Test
    fun `any failure bubbles up`() {
        val eventLogger: EventLogger = mock()
        val lastTxUpdater: LastTxUpdater = mock()

        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(any(), any(), any(), any(), any(), any())
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = false,
                    transaction = mock()
                )
            )
        }
        val sendDetails = SendDetails(
            XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
            199.456.lumens(),
            "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
            1.stroops()
        )
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            ),
            eventLogger = eventLogger,
            lastTxUpdater = lastTxUpdater
        ).sendFunds(
            sendDetails
        ).test()
            .assertComplete()
            .values().single() `should be equal to`
            SendFundsResult(
                sendDetails = sendDetails,
                errorCode = 1,
                confirmationDetails = null,
                hash = null
            )
        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
        verify(eventLogger, never()).logEvent(any())
        verify(lastTxUpdater, never()).updateLastTxTime()
    }

    @Test
    fun `can send with empty memo`() {
        val eventLogger: EventLogger = mock()
        val lastTxUpdater: LastTxUpdater = givenLastTxUpdater()
        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(127, 128.toByte(), 255.toByte()))
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(
                    source = keyPairEq(
                        KeyPair.fromSecretSeed(
                            "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
                        )
                    ),
                    destinationAccountId = eq(
                        "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3"
                    ),
                    amount = eq(199.456.lumens()),
                    memo = eq(org.stellar.sdk.Memo.none()),
                    timeout = any(),
                    perOperationFee = eq(256.stroops())
                )
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            ),
            feesFetcher = givenXlmFees(256.stroops()),
            eventLogger = eventLogger,
            lastTxUpdater = lastTxUpdater
        ).sendFunds(
            SendDetails(
                XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
                199.456.lumens(),
                "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
                256.stroops(),
                Memo.None
            )
        )
            .test()
            .assertNoErrors()
            .assertComplete()
            .values().single().hash `should be equal to` "7F80FF"

        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
        verify(eventLogger).logEvent(any())
        verify(lastTxUpdater).updateLastTxTime()
    }

    @Test
    fun `completes with failed update of last tx`() {
        val lastTxUpdater: LastTxUpdater = mock {
            on { updateLastTxTime() }.thenReturn(Completable.error(Exception()))
        }

        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(127, 128.toByte(), 255.toByte()))
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(
                    source = keyPairEq(
                        KeyPair.fromSecretSeed(
                            "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
                        )
                    ),
                    destinationAccountId = eq(
                        "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3"
                    ),
                    amount = eq(199.456.lumens()),
                    memo = eq(org.stellar.sdk.Memo.none()),
                    timeout = any(),
                    perOperationFee = eq(256.stroops())
                )
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            ),
            feesFetcher = givenXlmFees(256.stroops()),
            lastTxUpdater = lastTxUpdater
        ).sendFunds(
            SendDetails(
                XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
                199.456.lumens(),
                "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
                256.stroops(),
                Memo.None
            )
        )
            .test()
            .assertNoErrors()
            .assertComplete()
            .values().single().hash `should be equal to` "7F80FF"

        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
        verify(lastTxUpdater).updateLastTxTime()
    }

    @Test
    fun `any failure bubbles up - dry run`() {
        val horizonProxy: HorizonProxy = mock {
            on {
                dryRunTransaction(any(), any(), any(), any(), any(), any())
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = false,
                    transaction = mock()
                )
            )
        }
        val sendDetails = SendDetails(
            XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
            199.456.lumens(),
            "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
            1.stroops()
        )
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            )
        ).dryRunSendFunds(
            sendDetails
        ).test()
            .assertComplete()
            .values().single() `should be equal to`
            SendFundsResult(
                sendDetails = sendDetails,
                errorCode = 1,
                confirmationDetails = null,
                hash = null
            )
        horizonProxy.verifyJustTheOneDryRunNoSendsAndUpdate()
    }

    @Test
    fun `bad destination address - dry run`() {
        val horizonProxy: HorizonProxy = mock {
            on {
                dryRunTransaction(any(), any(), any(), any(), any(), any())
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = false,
                    transaction = mock(),
                    failureReason = HorizonProxy.FailureReason.BadDestinationAccountId
                )
            )
        }
        val sendDetails = SendDetails(
            XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
            199.456.lumens(),
            "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED4",
            1.stroops()
        )
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            )
        ).dryRunSendFunds(
            sendDetails
        ).test()
            .assertComplete()
            .values().single() `should be equal to`
            SendFundsResult(
                sendDetails = sendDetails,
                errorCode = 5,
                confirmationDetails = null,
                hash = null
            )
        horizonProxy.verifyJustTheOneDryRunNoSendsAndUpdate()
    }

    @Test
    fun `can send from a specific account`() {
        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(0, 1, 2, 3, 255.toByte()))
            on { fee }.thenReturn(101.stroops().toBigInteger().toLong())
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(
                    source = keyPairEq(
                        KeyPair.fromSecretSeed(
                            "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
                        )
                    ),
                    destinationAccountId = eq(
                        "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3"
                    ),
                    amount = eq(1.23.lumens()),
                    memo = any(),
                    timeout = any(),
                    perOperationFee = eq(256.stroops())
                )
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        val sendDetails = SendDetails(
            XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
            1.23.lumens(),
            "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
            256.stroops()
        )
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GBVO27UV2OXJFLFNXHMXOR5WRPKETM64XAQHUEKQ67W5LQDPZCDSTUTF",
                            label = "",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            givenPrivateForPublic(
                "GBVO27UV2OXJFLFNXHMXOR5WRPKETM64XAQHUEKQ67W5LQDPZCDSTUTF" to
                    "SBGS72YDKMO7K6YBDGXSD2U7BGFK3LRDCR36KNNXVL7N7L2OSEQSWO25",
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            ),
            feesFetcher = givenXlmFees(256.stroops())
        ).sendFunds(
            sendDetails
        ).test()
            .assertNoErrors()
            .assertComplete()
            .values().single() `should be equal to`
            SendFundsResult(
                sendDetails = sendDetails,
                errorCode = 0,
                hash = "00010203FF",
                confirmationDetails = SendConfirmationDetails(
                    sendDetails = sendDetails,
                    fees = 101.stroops()
                )
            )
        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
    }

    @Test
    fun `can dry run send from a specific account`() {
        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(0, 1, 2, 3, 255.toByte()))
            on { fee }.thenReturn(1000.stroops().toBigInteger().toLong())
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                dryRunTransaction(
                    source = keyPairEq(
                        KeyPair.fromAccountId(
                            "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"
                        )
                    ),
                    destinationAccountId = eq(
                        "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3"
                    ),
                    amount = eq(1.23.lumens()),
                    memo = any(),
                    perOperationFee = eq(500.stroops()),
                    timeout = any()
                )
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        val sendDetails = SendDetails(
            XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
            1.23.lumens(),
            "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
            500.stroops()
        )
        givenXlmDataManager(
            horizonProxy,
            givenMetaDataPrompt(
                XlmMetaData(
                    defaultAccountIndex = 0,
                    accounts = listOf(
                        XlmAccount(
                            publicKey = "GBVO27UV2OXJFLFNXHMXOR5WRPKETM64XAQHUEKQ67W5LQDPZCDSTUTF",
                            label = "",
                            archived = false
                        ),
                        XlmAccount(
                            publicKey = "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR",
                            label = "",
                            archived = false
                        )
                    ),
                    transactionNotes = emptyMap()
                )
            ),
            feesFetcher = givenXlmFees(500.stroops())
        ).dryRunSendFunds(
            sendDetails
        ).test()
            .assertNoErrors()
            .assertComplete()
            .values().single() `should be equal to`
            SendFundsResult(
                sendDetails = sendDetails,
                errorCode = 0,
                hash = "00010203FF",
                confirmationDetails = SendConfirmationDetails(
                    sendDetails = sendDetails,
                    fees = 1000.stroops()
                )
            )
        horizonProxy.verifyJustTheOneDryRunNoSendsAndUpdate()
    }

    @Test
    fun `when the address is not valid - do not throw`() {
        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(any(), any(), any(), any(), any(), any())
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = false,
                    failureReason = HorizonProxy.FailureReason.BadDestinationAccountId
                )
            )
        }
        givenXlmDataManager(
            horizonProxy,
            mock(),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            )
        ).sendFunds(
            SendDetails(
                XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
                1.23.lumens(),
                "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED4",
                1.stroops()
            )
        ).test()
            .assertNoErrors()
            .assertComplete()
            .values().single().apply {
                errorCode `should be equal to` 5
                success `should be` false
            }
        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
    }
}

class XlmDataManagerSendWithMemoTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `includes supplied memo`() {
        val memoText = org.stellar.sdk.Memo.text("Hi, this is the memo to add")
        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(0, 1, 2, 3, 255.toByte()))
            on { fee }.thenReturn(101.stroops().toBigInteger().toLong())
            on { memo }.thenReturn(memoText)
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                sendTransaction(any(), any(), any(), eq(memoText), any(), any())
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        val memo = Memo("")

        val memoMapper = mock<MemoMapper> {
            on { mapMemo(memo) }.thenReturn(memoText)
        }
        val dataManager = spy(
            givenXlmDataManager(
                horizonProxy,
                mock(),
                givenPrivateForPublic(
                    "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                        "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
                ),
                memoMapper
            )
        )
        whenever(dataManager.memoToEvent(memo)).thenReturn(object : CustomEventBuilder("event") {})

        dataManager.sendFunds(
            SendDetails(
                XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
                1.23.lumens(),
                "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
                1.stroops(),
                memo
            )
        ).test()
            .assertNoErrors()
            .assertComplete()
            .values().single().apply {
                success `should be` true
                transaction.memo `should be` memoText
            }
        horizonProxy.verifyJustTheOneSendAttemptAndUpdate()
    }

    @Test
    fun `includes supplied memo on dry run`() {
        val memoId = org.stellar.sdk.Memo.id(1234L)
        val transaction = mock<Transaction> {
            on { hash() }.thenReturn(byteArrayOf(0, 1, 2, 3, 255.toByte()))
            on { fee }.thenReturn(101.stroops().toBigInteger().toLong())
            on { memo }.thenReturn(memoId)
        }
        val horizonProxy: HorizonProxy = mock {
            on {
                dryRunTransaction(any(), any(), any(), eq(memoId), any(), any())
            }.thenReturn(
                HorizonProxy.SendResult(
                    success = true,
                    transaction = transaction
                )
            )
        }
        val memo = Memo("Hi, this is the memo to add", type = "id")
        val memoMapper = mock<MemoMapper> {
            on { mapMemo(memo) }.thenReturn(memoId)
        }
        givenXlmDataManager(
            horizonProxy,
            mock(),
            givenPrivateForPublic(
                "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR" to
                    "SCIB3NRLJR6BPQRF3WCSPBICSZIXNLGHKWDZZ32OA6TFOJJKWGNHOHIA"
            ),
            memoMapper
        ).dryRunSendFunds(
            SendDetails(
                XlmAccountReference("", "GB5INYM5XFJHAIQYXUQMGMQEM5KWBM4OYVLTWQI5JSQBRQKFYH3M3XWR"),
                1.23.lumens(),
                "GDKDDBJNREDV4ITL65Z3PNKAGWYJQL7FZJSV4P2UWGLRXI6AWT36UED3",
                1.stroops(),
                memo
            )
        ).test()
            .assertNoErrors()
            .assertComplete()
            .values().single().apply {
                errorCode `should be equal to` 0
                success `should be` true
                transaction.memo `should be` memoId
            }
        horizonProxy.verifyJustTheOneDryRunNoSendsAndUpdate()
    }
}

private fun HorizonProxy.verifyJustTheOneSendAttemptAndUpdate() {
    verify(this).sendTransaction(any(), any(), any(), any(), any(), any())
    verify(this).update(any())
    verifyNoMoreInteractions(this)
}

private fun HorizonProxy.verifyJustTheOneDryRunNoSendsAndUpdate() {
    verify(this).dryRunTransaction(any(), any(), any(), any(), any(), any())
    verify(this).update(any())
    verifyNoMoreInteractions(this)
}

private fun <T> Single<T>.testSingle() = test().values().single()

private fun <T> Maybe<T>.testSingle() = test().values().single()

private fun givenBalances(
    vararg balances: Pair<String, CryptoValue>
): HorizonProxy {
    val horizonProxy: HorizonProxy = mock()
    balances.forEach { pair ->
        whenever(horizonProxy.getBalance(pair.first)).thenReturn(pair.second)
    }
    return horizonProxy
}

private fun givenBalancesAndMinimums(
    vararg balances: Pair<String, BalanceAndMin>
): HorizonProxy {
    val horizonProxy: HorizonProxy = mock()
    balances.forEach { pair ->
        whenever(horizonProxy.getBalanceAndMin(pair.first)).thenReturn(pair.second)
    }
    return horizonProxy
}

private fun givenTransactions(
    fee: Long,
    vararg transactions: Pair<String, List<OperationResponse>>
): HorizonProxy {
    val horizonProxy: HorizonProxy = mock()
    val mockTx: TransactionResponse = mock { on { feeCharged }.thenReturn(fee) }
    transactions
        .forEach { pair ->
            whenever(horizonProxy.getTransactionList(pair.first)).thenReturn(pair.second)
        }
    whenever(horizonProxy.getTransaction(any())).thenReturn(mockTx)
    return horizonProxy
}

private fun givenMetaDataMaybe(metaData: XlmMetaData): XlmMetaDataInitializer =
    mock {
        on { initWalletMaybe }.thenReturn(
            Maybe.just(
                metaData
            ).subscribeOn(Schedulers.io())
        )
    }

private fun givenMetaDataPrompt(metaData: XlmMetaData): XlmMetaDataInitializer =
    mock {
        on { initWalletMaybePrompt }.thenReturn(
            Maybe.just(
                metaData
            ).subscribeOn(Schedulers.io())
        )
    }

private fun givenNoMetaData(): XlmMetaDataInitializer =
    mock {
        on { initWalletMaybe }.thenReturn(
            Maybe.empty<XlmMetaData>()
                .subscribeOn(Schedulers.io())
        )
        on { initWalletMaybePrompt }.thenReturn(
            Maybe.empty<XlmMetaData>()
                .subscribeOn(Schedulers.io())
        )
    }

private fun verifyNoInteractionsBeforeSubscribe(function: XlmDataManager.() -> Unit) {
    val horizonProxy = mock<HorizonProxy>()
    val metaDataInitializer = mock<XlmMetaDataInitializer>()
    val fees = mock<XlmFeesFetcher>()
    val xlmDataManager = givenXlmDataManager(
        horizonProxy,
        metaDataInitializer,
        feesFetcher = fees
    )
    function(xlmDataManager)
    verifyZeroInteractions(horizonProxy)
    verifyZeroInteractions(metaDataInitializer)
    verifyZeroInteractions(fees)
}

private fun givenXlmDataManager(
    horizonProxy: HorizonProxy = mock(),
    metaDataInitializer: XlmMetaDataInitializer = mock(),
    secretAccess: XlmSecretAccess = givenNoExpectedSecretAccess(),
    memoMapper: MemoMapper = givenAllMemosMapToNone(),
    feesFetcher: XlmFeesFetcher = givenXlmFees(999.stroops()),
    timeoutFetcher: XlmTransactionTimeoutFetcher = givenTimeoutFetcher(10),
    lastTxUpdater: LastTxUpdater = givenLastTxUpdater(),
    eventLogger: EventLogger = mock()
): XlmDataManager {

    return XlmDataManager(
        horizonProxy,
        metaDataInitializer,
        secretAccess,
        memoMapper,
        feesFetcher,
        timeoutFetcher,
        lastTxUpdater,
        eventLogger,
        urlFetcher(),
        ""
    )
}

fun urlFetcher(): XlmHorizonUrlFetcher =
    mock {
        on { xlmHorizonUrl(any()) }.thenReturn(Single.just(""))
    }

private fun givenLastTxUpdater(): LastTxUpdater =
    mock {
        on { this.updateLastTxTime() }.thenReturn(Completable.complete())
    }

private fun givenTimeoutFetcher(timeout: Long): XlmTransactionTimeoutFetcher =
    mock {
        on { this.transactionTimeout() }.thenReturn(Single.just(timeout))
    }

private fun givenXlmFees(perOperationFee: CryptoValue): XlmFeesFetcher =
    mock {
        on { this.operationFee(any()) }.thenReturn(Single.just(perOperationFee))
    }

private fun givenAllMemosMapToNone(): MemoMapper =
    mock {
        on { mapMemo(anyOrNull()) }.thenReturn(org.stellar.sdk.Memo.none())
    }

private fun givenNoExpectedSecretAccess(): XlmSecretAccess =
    mock {
        on { getPrivate(any(), any()) }.doThrow(RuntimeException("Not expected"))
    }

private fun givenPrivateForPublic(vararg pairs: Pair<String, String>): XlmSecretAccess {
    val mock: XlmSecretAccess = mock()
    for (pair in pairs) {
        whenever(mock.getPrivate(HorizonKeyPair.Public(pair.first), null)).thenReturn(
            Single.just(
                HorizonKeyPair.Private(
                    pair.first,
                    pair.second.toCharArray()
                )
            )
        )
    }
    return mock
}
