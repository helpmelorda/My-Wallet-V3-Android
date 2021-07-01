package info.blockchain.wallet.multiaddress

import com.blockchain.api.bitcoin.data.Output
import com.blockchain.api.bitcoin.data.Transaction
import info.blockchain.wallet.bip44.HDChain
import java.math.BigInteger

internal fun Transaction.toTransactionSummary(
    ownAddresses: MutableList<String>,
    startingBlockHeight: Int,
    latestBlock: Int
): TransactionSummary? {

    val blockHeight: Long = this.blockHeight ?: return null
    if (blockHeight != 0L && blockHeight < startingBlockHeight) {
        // Filter out txs before blockHeight (mainly for BCH)
        // Block height will be 0 until included in a block
        return null
    }

    var transactionType = determineTxType(this)

    val inputsMap = HashMap<String, BigInteger>()
    val inputsXpubMap = HashMap<String, String>()

    for (input in this.inputs) {
        val prevOut = input.prevOut
        if (prevOut != null) {
            val inputAddr = prevOut.addr
            val inputValue = prevOut.value
            if (inputAddr != null) {
                // Transaction from HD account
                prevOut.xpub?.let {
                    // xpubBody will only show if it belongs to our account
                    // inputAddr belongs to our own account - add it, it's a transfer/send
                    ownAddresses.add(inputAddr)
                    inputsXpubMap[inputAddr] = it.address
                }

                // Transaction from HD account
                val xpubBody = prevOut.xpub
                if (xpubBody != null) {
                    // xpubBody will only show if it belongs to our account
                    // inputAddr belongs to our own account - add it, it's a transfer/send
                    ownAddresses.add(inputAddr)
                    inputsXpubMap[inputAddr] = xpubBody.address
                }

                // Keep track of inputs
                val existingBalance: BigInteger = inputsMap[inputAddr] ?: BigInteger.ZERO
                inputsMap[inputAddr] = existingBalance.add(inputValue)
            } else {
                // No input address available
                inputsMap[TransactionSummary.ADDRESS_DECODE_ERROR] = inputValue
            }
        } // else Newly generated coin
    }

    // Process outputs
    val outputsXpubMap = HashMap<String, String>()
    val taggedOuts = this.out.map { out ->
        out.toTaggedOutput(
            inputsMap = inputsMap,
            ownAddresses = ownAddresses,
            outputsXpubMap = outputsXpubMap
        )
    }

    val outputsMap = HashMap<String, BigInteger>()
    val changeMap = HashMap<String, BigInteger>()

    when {
        isExternalSend(transactionType, taggedOuts) ->
            processExternalSend(
                taggedOutput = taggedOuts,
                outputsMap = outputsMap,
                changeMap = changeMap
            )
        isInternalSend(transactionType, taggedOuts) -> {
            processInternalSend(
                taggedOutput = taggedOuts,
                outputsMap = outputsMap,
                changeMap = changeMap
            )
            transactionType = TransactionSummary.TransactionType.TRANSFERRED
        }
        else -> {
            // It's a "receive"
            processReceive(
                taggedOutput = taggedOuts,
                outputsMap = outputsMap,
                changeMap = changeMap
            )
        }
    }

    // Remove input addresses not ours
    filterOwnedAddresses(
        ownAddresses,
        inputsMap,
        outputsMap,
        transactionType
    )

    return TransactionSummary(
        transactionType = transactionType,
        hash = this.hash ?: "",
        time = this.time,
        fee = this.fee ?: BigInteger.ZERO,
        inputsXpubMap = inputsXpubMap,
        outputsXpubMap = outputsXpubMap,
        // Track values for inout qnd output addresses
        inputsMap = inputsMap,
        outputsMap = outputsMap,
        isDoubleSpend = this.isDoubleSpend,
        total = if (transactionType == TransactionSummary.TransactionType.RECEIVED) {
            calculateTotalReceived(outputsMap)
        } else {
            calculateTotalSent(
                inputsMap = inputsMap,
                changeMap = changeMap,
                fee = this.fee ?: BigInteger.ZERO,
                direction = transactionType
            )
        },
        confirmations =
            if (latestBlock > 0 && blockHeight > 0) {
                (latestBlock - blockHeight + 1).toInt()
            } else {
                0
            }
    )
}

private fun determineTxType(
    tx: Transaction
): TransactionSummary.TransactionType =
    when {
        tx.result.add(tx.fee).signum() == 0 -> TransactionSummary.TransactionType.TRANSFERRED
        tx.result.signum() > 0 -> TransactionSummary.TransactionType.RECEIVED
        else -> TransactionSummary.TransactionType.SENT
    }

private fun isExternalSend(
    type: TransactionSummary.TransactionType,
    taggedOuts: List<TaggedOutput>
): Boolean = (type == TransactionSummary.TransactionType.SENT) &&
        taggedOuts.any { it.type == OutputType.EXTERNAL }

private fun isInternalSend(
    type: TransactionSummary.TransactionType,
    taggedOuts: List<TaggedOutput>
): Boolean = (type == TransactionSummary.TransactionType.SENT) &&
    taggedOuts.any { it.type == OutputType.INTERNAL }

private fun processExternalSend(
    taggedOutput: List<TaggedOutput>,
    outputsMap: MutableMap<String, BigInteger>,
    changeMap: MutableMap<String, BigInteger>
) {
    // If we're here, we were sending to an external address.
    // Count INTERNAL and CHANGE as change
    // Ignore UNKNOWN
    outputsMap.populateFromOutputs(
        taggedOutput,
        setOf(OutputType.EXTERNAL)
    )

    changeMap.populateFromOutputs(
        taggedOutput,
        setOf(OutputType.CHANGE, OutputType.INTERNAL)
    )
}

private fun processInternalSend(
    taggedOutput: List<TaggedOutput>,
    outputsMap: MutableMap<String, BigInteger>,
    changeMap: MutableMap<String, BigInteger>
) {
    // If we're here, we were running a transfer to an internal address.
    // Count INTERNAL as output and CHANGE as change
    // Ignore UNKNOWN, there are no EXTERNAL
    check(taggedOutput.none { it.type == OutputType.EXTERNAL })

    outputsMap.populateFromOutputs(
        taggedOutput,
        setOf(OutputType.INTERNAL)
    )

    changeMap.populateFromOutputs(
        taggedOutput,
        setOf(OutputType.CHANGE)
    )
}

private fun processReceive(
    taggedOutput: List<TaggedOutput>,
    outputsMap: MutableMap<String, BigInteger>,
    changeMap: MutableMap<String, BigInteger>
) {
    // Count INTERNAL and EXTERNAL as output and CHANGE as change
    // Ignore UNKNOWN
    outputsMap.populateFromOutputs(
        taggedOutput,
        setOf(OutputType.INTERNAL, OutputType.EXTERNAL)
    )

    changeMap.populateFromOutputs(
        taggedOutput,
        setOf(OutputType.CHANGE)
    )
}

private fun MutableMap<String, BigInteger>.populateFromOutputs(
    taggedOutput: List<TaggedOutput>,
    outputTypes: Set<OutputType>
) {
    taggedOutput.filter { it.type in outputTypes }
        .forEach {
            val existingBalance = this[it.address] ?: BigInteger.ZERO
            this[it.address] = existingBalance.add(it.value)
        }
}

private data class TaggedOutput(
    val type: OutputType,
    val value: BigInteger,
    val address: String
)

private enum class OutputType {
    INTERNAL,
    CHANGE,
    EXTERNAL,
    UNKNOWN
}

private fun Output.toTaggedOutput(
    inputsMap: Map<String, BigInteger>,
    ownAddresses: MutableList<String>,
    outputsXpubMap: MutableMap<String, String>
): TaggedOutput {
    val outputAddr = this.addr
    val outputValue = this.value

    if (outputAddr != null) {
        val xpubBody = this.xpub
        if (xpubBody != null) {
            // inputAddr belongs to our own account - add it
            ownAddresses.add(outputAddr)
            outputsXpubMap[outputAddr] = xpubBody.address
            return if (xpubBody.derivationPath.startsWith(HDChain.RECEIVE_CHAIN_DERIVATION_PREFIX)) {
                TaggedOutput(
                    type = OutputType.INTERNAL,
                    address = outputAddr,
                    value = outputValue
                )
            } else {
                TaggedOutput(
                    type = OutputType.CHANGE,
                    address = outputAddr,
                    value = outputValue
                )
            }
        } else {
            // If we own this address and it's not change coming back, it's a transfer
            return when {
                ownAddresses.contains(outputAddr) && !inputsMap.containsKey(outputAddr) -> {
                    TaggedOutput(
                        type = OutputType.INTERNAL,
                        address = outputAddr,
                        value = outputValue
                    )
                }
                inputsMap.containsKey(outputAddr) ->
                    TaggedOutput(
                        type = OutputType.CHANGE,
                        address = outputAddr,
                        value = outputValue
                    )
                else ->
                    TaggedOutput(
                        type = OutputType.EXTERNAL,
                        address = outputAddr,
                        value = outputValue
                    )
            }
        }
    }

    // No output address available
    return TaggedOutput(
        type = OutputType.UNKNOWN,
        address = TransactionSummary.ADDRESS_DECODE_ERROR,
        value = outputValue
    )
}

private fun filterOwnedAddresses(
    ownAddresses: List<String>,
    inputsMap: HashMap<String, BigInteger>,
    outputsMap: HashMap<String, BigInteger>,
    transactionType: TransactionSummary.TransactionType
) {
    var iterator: MutableIterator<Map.Entry<String, BigInteger>> = inputsMap.entries.iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        if (!ownAddresses.contains(item.key) && transactionType == TransactionSummary.TransactionType.SENT) {
            iterator.remove()
        }
    }

    iterator = outputsMap.entries.iterator()
    while (iterator.hasNext()) {
        val item = iterator.next()
        if (!ownAddresses.contains(item.key) && transactionType == TransactionSummary.TransactionType.RECEIVED) {
            iterator.remove()
        }
    }
}

private fun calculateTotalReceived(outputsMap: HashMap<String, BigInteger>): BigInteger =
    outputsMap.values.sumOf { it }

private fun calculateTotalSent(
    inputsMap: HashMap<String, BigInteger>,
    changeMap: HashMap<String, BigInteger>,
    fee: BigInteger,
    direction: TransactionSummary.TransactionType
): BigInteger {
    var total = BigInteger.ZERO

    total += inputsMap.values.sumOf { it }
    total -= changeMap.values.sumOf { it }

    if (direction == TransactionSummary.TransactionType.TRANSFERRED) {
        total = total.subtract(fee)
    }
    return total
}
