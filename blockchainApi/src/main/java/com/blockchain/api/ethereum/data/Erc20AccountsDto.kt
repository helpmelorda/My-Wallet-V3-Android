@file:UseSerializers(BigIntSerializer::class)
package com.blockchain.api.ethereum.data

import com.blockchain.api.serializers.BigIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigInteger

@Serializable
internal data class Erc20TokenBalanceDto(
    @SerialName("accountHash")
    val accountHash: String,
    @SerialName("tokenHash")
    val contractAddress: String,
    @SerialName("balance")
    val balance: BigInteger,
    @SerialName("totalSent")
    val totalSent: BigInteger,
    @SerialName("totalReceived")
    val totalReceived: BigInteger,
    @SerialName("decimals")
    val precisionDp: Int, // should tally with the precisionDp defined in the asset fetch call
    @SerialName("transferCount")
    val transferCount: Int,
    @SerialName("tokenSymbol")
    val ticker: String
)

@Serializable
internal data class Erc20AccountsDto(
    @SerialName("tokenAccounts")
    val balances: List<Erc20TokenBalanceDto> = emptyList()
)