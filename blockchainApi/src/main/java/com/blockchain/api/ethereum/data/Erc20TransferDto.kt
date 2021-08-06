@file:UseSerializers(BigIntSerializer::class)
package com.blockchain.api.ethereum.data

import com.blockchain.api.serializers.BigIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigInteger

@Serializable
internal class Erc20TransferDto(
    @SerialName("logIndex")
    val logIndex: String, // TODO: What this?
    @SerialName("tokenHash")
    val contractAddress: String,
    @SerialName("from")
    val fromAddress: String,
    @SerialName("to")
    val toAddress: String,
    @SerialName("value")
    val value: BigInteger,
    @SerialName("decimals")
    val precisionDp: Int,
    @SerialName("blockHash")
    val blockHash: String,
    @SerialName("transactionHash")
    val txHash: String,
    @SerialName("blockNumber")
    val blockNumber: BigInteger,
    @SerialName("idxFrom")
    val idxFrom: String, // TODO: What this?
    @SerialName("idxTo")
    val idxTo: String, // TODO: What this?
    @SerialName("accountIdxFrom")
    val accountIdxFrom: String, // TODO: What this?
    @SerialName("accountIdxTo")
    val accountIdxTo: String, // TODO: What this?
    @SerialName("timestamp")
    val timestamp: Long
)

@Serializable
internal data class Erc20AccountTransfersDto(
    @SerialName("transfers")
    val transfers: List<Erc20TransferDto> = emptyList(),
    @SerialName("page")
    val page: Int,
    @SerialName("size")
    val size: Int
)
