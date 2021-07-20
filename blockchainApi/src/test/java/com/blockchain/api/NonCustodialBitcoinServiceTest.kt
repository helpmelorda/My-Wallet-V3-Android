package com.blockchain.api

import com.blockchain.api.bitcoin.BitcoinApi
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.AddressSummary
import com.blockchain.api.bitcoin.data.BalanceDto
import com.blockchain.api.bitcoin.data.Info
import com.blockchain.api.bitcoin.data.Input
import com.blockchain.api.bitcoin.data.MultiAddress
import com.blockchain.api.bitcoin.data.MultiAddressBalance
import com.blockchain.api.bitcoin.data.Output
import com.blockchain.api.bitcoin.data.RawBlock
import com.blockchain.api.bitcoin.data.Transaction
import com.blockchain.api.bitcoin.data.UnspentOutputDto
import com.blockchain.api.bitcoin.data.UnspentOutputsDto
import com.blockchain.api.bitcoin.data.XpubDto
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.HttpException
import retrofit2.Response

class NonCustodialBitcoinServiceTest {
    private val apiCode = "12345"
    private val api = mock<BitcoinApi>()
    private val client: NonCustodialBitcoinService = NonCustodialBitcoinService(api, apiCode)

    @Test
    fun testGetBalance() {
        val address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        val address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7" +
            "W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        val listLegacy = listOf(address1, address2)
        val listBech32 = emptyList<String>()
        val legacyAddresses = listLegacy.joinToString(",")
        val bech32Addresses = listBech32.joinToString(",")

        val balanceResponse = mockApiResponse(
            mapOf(
                "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqzt" +
                    "us7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
                    to BalanceDto(
                    finalBalance = "20000",
                    txCount = 1,
                    totalReceived = "20000"
                ),
                "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW" to
                    BalanceDto(
                        finalBalance = "0",
                        txCount = 2,
                        totalReceived = "20000"
                    )
            )
        )
        whenever(
            api.getBalance(
                NonCustodialBitcoinService.BITCOIN,
                legacyAddresses,
                bech32Addresses,
                4,
                apiCode
            )
        ).thenReturn(balanceResponse)

        val result = client.getBalance(
            NonCustodialBitcoinService.BITCOIN,
            listLegacy,
            listBech32,
            NonCustodialBitcoinService.BalanceFilter.All
        )
        verify(api).getBalance(
            NonCustodialBitcoinService.BITCOIN,
            legacyAddresses,
            bech32Addresses,
            4,
            apiCode
        )
        val response = result.execute().body()!!
        assertNotNull(response)
        val balance1 = response[address1]
        assertEquals(balance1?.finalBalance, "0")
        assertEquals(balance1?.txCount, 2L)
        assertEquals(balance1?.totalReceived, "20000")
        val balance2 = response[address2]
        assertEquals(balance2?.finalBalance, "20000")
        assertEquals(balance2?.txCount, 1L)
        assertEquals(balance2?.totalReceived, "20000")
    }

    @Test
    fun testGetMultiAddress_BTC() {
        val address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        val address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W" +
            "7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"

        val list = listOf(address1, address2)
        val listP2SH = emptyList<String>()

        val legacyAddresses = list.joinToString("|")
        val bech32Addresses = listP2SH.joinToString("|")

        val response = MultiAddress(
            MultiAddressBalance(
                totalReceived = 40000.toBigInteger(),
                totalSent = 20000.toBigInteger(),
                finalBalance = 20000.toBigInteger()
            ),
            txs = listOf(
                Transaction(
                    hash = "72743ce381c5eab3a23535ef158c6e6b435ebfc8d493d387b90aee1818b47a2e",
                    ver = 1, lockTime = 0, blockHeight = 410371L,
                    relayedBy = "0.0.0.0", result = 20000.toBigInteger(),
                    size = 226, time = 1462466670, txIndex = 145849898,
                    vinSz = 1, voutSz = 2, isDoubleSpend = false,
                    inputs = listOf(
                        Input(
                            sequence = 4294967295L,
                            script = "483045022100e766eda1bcccae4d0076dc0924" +
                                "2a42492b39e31f7e83a11263a93d75f3cd86f6022069665bb861898a" +
                                "b198392698c5b21caead19ff535df223c0bb63a978b1221ac2012102a4cc88b940db6a" +
                                "00487b2638cae13dd3c7853ced968c99b1187eeceea0f91ceb",
                            prevOut = Output(
                                isSpent = true, txIndex = 145808878L, addr = "1GrYvVX76JMMeU32PCoyndaeYU5odDGAu3",
                                value = 240240L.toBigInteger(), count = 1L,
                                script = "76a914ade8ea8fa072aafc8caf66af4ea815dd1e3dfe6f88ac"
                            )
                        )
                    ),
                    out = listOf(
                        Output(
                            isSpent = false, txIndex = 145849898L, addr = "19tEaovasXx75vjuwYqziZSJg7b3u1MTQt",
                            value = 20000L.toBigInteger(), count = 0L,
                            script = "76a91461718f0b60dc85dc09c8e59d0ddd6901bab900da88ac",
                            xpub = XpubDto(
                                "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1b" +
                                    "RRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn", derivationPath = "M/0/0"
                            )

                        )
                    )
                )
            ),
            addresses = listOf(
                AddressSummary(
                    "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW",
                    2,
                    20000.toBigInteger(),
                    20000.toBigInteger(),
                    0.toBigInteger()
                ),
                AddressSummary(
                    "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqzt" +
                        "us7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn",
                    1,
                    20000.toBigInteger(),
                    0.toBigInteger(),
                    20000.toBigInteger()
                )
            ),
            info = Info(latestBlock = RawBlock(prevBlock = "1234"))
        )

        val balanceResponse = mockApiResponse(response)

        whenever(
            api.getMultiAddress(
                NonCustodialBitcoinService.BITCOIN,
                legacyAddresses,
                bech32Addresses,
                20,
                0,
                4,
                null,
                apiCode
            )
        ).thenReturn(balanceResponse)

        val callClient = client.getMultiAddress(
            NonCustodialBitcoinService.BITCOIN,
            list,
            listP2SH,
            null,
            NonCustodialBitcoinService.BalanceFilter.All,
            20,
            0
        )

        verify(api).getMultiAddress(
            NonCustodialBitcoinService.BITCOIN,
            legacyAddresses,
            bech32Addresses,
            20,
            0,
            4,
            null,
            apiCode
        )

        val multiAddress = callClient.execute().body()

        val balance = multiAddress!!.multiAddressBalance
        assertEquals(balance.txCount, 0)
        assertEquals(balance.txCountFiltered, 0)
        assertEquals(balance.totalReceived.toLong(), 40000)
        assertEquals(balance.totalSent.toLong(), 20000)
        assertEquals(balance.finalBalance.toLong(), 20000)

        // Addresses
        val firstSummary = multiAddress.addresses[0]
        assertEquals(
            firstSummary.address,
            "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        )
        assertEquals(firstSummary.txCount, 2)
        assertEquals(firstSummary.totalReceived.toLong(), 20000)
        assertEquals(firstSummary.totalSent.toLong(), 20000)
        assertEquals(firstSummary.finalBalance.toLong(), 0)
        val secondSummary = multiAddress.addresses[1]
        assertEquals(
            secondSummary.address,
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48K" +
                "xuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        )
        assertEquals(secondSummary.txCount, 1)
        assertEquals(secondSummary.totalReceived.toLong(), 20000)
        assertEquals(secondSummary.totalSent.toLong(), 0)
        assertEquals(secondSummary.finalBalance.toLong(), 20000)

        // Txs
        val firstTx = multiAddress.txs[0]
        assertEquals(
            firstTx.hash,
            "72743ce381c5eab3a23535ef158c6e6b435ebfc8d493d387b90aee1818b47a2e"
        )
        assertEquals(firstTx.ver, 1)
        assertEquals(firstTx.lockTime, 0)
        assertEquals(firstTx.blockHeight, 410371L)
        assertEquals(firstTx.relayedBy, "0.0.0.0")
        assertEquals(firstTx.result.toLong(), 20000)
        assertEquals(firstTx.size, 226)
        assertEquals(firstTx.time, 1462466670)
        assertEquals(firstTx.txIndex, 145849898)
        assertEquals(firstTx.vinSz, 1)
        assertEquals(firstTx.voutSz, 2)
        assertFalse(firstTx.isDoubleSpend)

        val firstInput = firstTx.inputs[0]
        assertEquals(
            firstInput.sequence,
            4294967295L
        )
        assertEquals(
            firstInput.script,
            "483045022100e766eda1bcccae4d0076dc09242a42492b39e31f7e83a11263a93d75f3cd86f6022" +
                "069665bb861898ab198392698c5b21caead19ff535df223c0bb63a978b1221ac2012102a4cc88b940" +
                "db6a00487b2638cae13dd3c7853ced968c99b1187eeceea0f91ceb"
        )
        assertEquals(firstInput.prevOut?.isSpent, true)
        assertEquals(firstInput.prevOut?.txIndex, 145808878L)
        assertEquals(firstInput.prevOut?.type, 0)
        assertEquals(firstInput.prevOut?.addr, "1GrYvVX76JMMeU32PCoyndaeYU5odDGAu3")
        assertEquals(firstInput.prevOut?.value?.toLong(), 240240L)
        assertEquals(firstInput.prevOut?.count, 1L)
        assertEquals(firstInput.prevOut?.script, "76a914ade8ea8fa072aafc8caf66af4ea815dd1e3dfe6f88ac")

        val firstOutput = firstTx.out[0]
        assertFalse(firstOutput.isSpent)
        assertEquals(firstOutput.txIndex, 145849898L)
        assertEquals(firstOutput.type.toLong(), 0)
        assertEquals(firstOutput.addr, "19tEaovasXx75vjuwYqziZSJg7b3u1MTQt")
        assertEquals(firstOutput.value.toLong(), 20000L)
        assertEquals(firstOutput.count, 0L)
        assertEquals(firstOutput.script, "76a91461718f0b60dc85dc09c8e59d0ddd6901bab900da88ac")
        assertEquals(
            firstOutput.xpub?.address,
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1b" +
                "RRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        )
        assertEquals(firstOutput.xpub?.derivationPath, "M/0/0")
    }

    @Test
    fun testUnspentOutputs_BTC() {
        val address1 = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T"
        val address2 =
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W" +
                "7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"

        val listLegacy = listOf(address1, address2)
        val listBech32 = emptyList<String>()

        val legacyAddresses = listLegacy.joinToString("|")
        val bech32Addresses = listBech32.joinToString("|")

        val expected = UnspentOutputDto(txHash = "123")
        val expectedResponse = UnspentOutputsDto("As", listOf(expected))

        whenever(
            api.getUnspent(
                NonCustodialBitcoinService.BITCOIN,
                legacyAddresses,
                bech32Addresses,
                10,
                50,
                apiCode
            )
        ).thenReturn(Single.just(expectedResponse))

        client.getUnspentOutputs(
            NonCustodialBitcoinService.BITCOIN,
            listLegacy,
            listBech32,
            10,
            50
        ).test()
            .assertComplete()
            .assertValue { (notice, unspentOutputs) ->
                notice != null && unspentOutputs[0] == expected
            }
    }

    @Test
    fun testUnspentOutputs_BTC_no_UTXOs() {
        val address = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T"

        val listLegacy = listOf(address)
        val listBech32 = emptyList<String>()

        val legacyAddresses = listLegacy.joinToString("|")
        val bech32Addresses = listBech32.joinToString("|")

        val mockException = mock<HttpException> {
            on { code() }.thenReturn(500)
        }
        whenever(
            api.getUnspent(
                NonCustodialBitcoinService.BITCOIN,
                legacyAddresses,
                bech32Addresses,
                10,
                50,
                apiCode
            )
        ).thenReturn(Single.error(mockException))

        client.getUnspentOutputs(
            NonCustodialBitcoinService.BITCOIN,
            listLegacy,
            listBech32,
            10,
            50
        ).test()
            .assertComplete()
            .assertValue { (notice, unspentOutputs) ->
                notice == null && unspentOutputs.isEmpty()
            }
    }

    @Test
    fun testUnspentOutputs_BTC_server_error() {
        val address = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T"

        val listLegacy = listOf(address)
        val listBech32 = emptyList<String>()

        val legacyAddresses = listLegacy.joinToString("|")
        val bech32Addresses = listBech32.joinToString("|")

        val mockException = mock<HttpException> {
            on { code() }.thenReturn(501)
        }
        whenever(
            api.getUnspent(
                NonCustodialBitcoinService.BITCOIN,
                legacyAddresses,
                bech32Addresses,
                10,
                50,
                apiCode
            )
        ).thenReturn(Single.error(mockException))

        client.getUnspentOutputs(
            NonCustodialBitcoinService.BITCOIN,
            listLegacy,
            listBech32,
            10,
            50
        ).test()
            .assertError { e -> e is HttpException }
    }

    @Test
    fun testPushTx_BTC() {
        val txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006a47304" +
            "4022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a890a6ee0fb0a03" +
            "a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e829315c941ae82cf042409" +
            "1a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a175cfb381ffeb788ac00000000"
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(200, responseBody)
        val call = mock<Call<ResponseBody>>()
        whenever(call.execute()).thenReturn(response)
        whenever(api.pushTx("btc", txHash, apiCode)).thenReturn(call)
        val callClient = client.pushTx("btc", txHash)
        val execution = callClient.execute()
        assertTrue(execution.isSuccessful)
    }

    @Test
    fun testPushTx_BCH() {
        val txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006" +
            "a473044022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a8" +
            "90a6ee0fb0a03a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e82" +
            "9315c941ae82cf0424091a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a" +
            "175cfb381ffeb788ac00000000"
        val responseBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        val response = Response.success(200, responseBody)
        val call = mock<Call<ResponseBody>>()
        whenever(call.execute()).thenReturn(response)
        whenever(api.pushTx("bch", txHash, apiCode)).thenReturn(call)
        val callClient = client.pushTx("bch", txHash)
        val execution = callClient.execute()
        assertTrue(execution.isSuccessful)
    }

    private fun <T> mockApiResponse(responseBody: T): Call<T> {
        val response: Response<T> = mock {
            on { body() }.thenReturn(responseBody)
            on { isSuccessful }.thenReturn(true)
        }
        return mock {
            on { execute() }.thenReturn(response)
        }
    }
}