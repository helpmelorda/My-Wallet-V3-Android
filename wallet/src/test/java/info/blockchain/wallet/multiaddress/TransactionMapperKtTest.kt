package info.blockchain.wallet.multiaddress

import com.blockchain.api.bitcoin.data.Input
import com.blockchain.api.bitcoin.data.Output
import com.blockchain.api.bitcoin.data.Transaction
import com.blockchain.api.bitcoin.data.XpubDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionMapperKtTest {

    private val xpubs = listOf(
        "xpub6CDBEbdAdWfnQ6qXJ9diapcdmSF4kxoPGwx2SMzPKS5tKTpT6XPnswneuFLt" +
            "Qpb1PCeKzjzMDZNBJ4msC9SJGaPr5icrZvWNXMU5PETCAdM",
        "xpub6CWkHJauBHNWJaqmKUp88bFAQBWYuccvK6DfkGW5T1jSUp5S8YWh3hQsYQrj" +
            "VoqD2KScTDuYFqiMbBoX3TGtaTuRYMCw7Jf3Es7vdiJGdQF"
    )
    private val startingBlockHeight = 0
    private val latestBlock = 689215

    @Test
    fun processFirstSegwitTransaction() {
        //
        val tx = Transaction(
            hash = "7a6925ed7fa64765a8308cabfd8b964a6d8cd11db10f594dd3fb9518836a542f",
            blockHeight = 688470,
            result = (-43760).toBigInteger(),
            fee = 4862.toBigInteger(),
            time = 1624350848,
            isDoubleSpend = false,
            inputs = listOf(
                Input(
                    sequence = 4294967295,
                    prevOut = Output(
                        isSpent = true,
                        addr = "1EKqhuyQKMeG8wmTZe5Fuezwtt2Db2h8Rm",
                        value = 85245.toBigInteger(),
                        count = 0,
                        xpub = XpubDto(
                            address = "xpub6CDBEbdAdWfnQ6qXJ9diapcdmSF4kxoPGwx2SMzPKS5tKTpT6X" +
                                "PnswneuFLtQpb1PCeKzjzMDZNBJ4msC9SJGaPr5icrZvWNXMU5PETCAdM",
                            derivationPath = "M/0/0"
                        )
                    )
                )
            ),
            out = listOf(
                Output(
                    isSpent = false,
                    addr = "bc1qdec4ga0z24f2nq2chcfckm0edkqdx6dqr6a0m7",
                    value = 38898.toBigInteger(),
                    count = 0,
                    xpub = null
                ),
                Output(
                    isSpent = true,
                    addr = "bc1qj2lj59wquze86lk6sc6sj5av5fwgq204avlhv3",
                    value = 41485.toBigInteger(),
                    count = 1,
                    xpub = XpubDto(
                        address = "xpub6CWkHJauBHNWJaqmKUp88bFAQBWYuccvK6DfkGW5T1jSUp5S8YWh3hQsY" +
                            "QrjVoqD2KScTDuYFqiMbBoX3TGtaTuRYMCw7Jf3Es7vdiJGdQF",
                        derivationPath = "M/0/0"
                    )
                )
            )
        )

        // Act
        val result = tx.toTransactionSummary(
            ownAddresses = xpubs.toMutableList(),
            startingBlockHeight = startingBlockHeight,
            latestBlock = latestBlock
        )

        // Assert
        assertNotNull(result)
        assertEquals(TransactionSummary.TransactionType.SENT, result?.transactionType)
        assertEquals(43760.toBigInteger(), result?.total)
        // The change address should not be present:
        assertEquals(1, result?.outputsMap?.size)
        assertTrue(result!!.outputsMap.containsKey("bc1qdec4ga0z24f2nq2chcfckm0edkqdx6dqr6a0m7"))
    }
}