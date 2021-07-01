package info.blockchain.wallet.multiaddress

import java.math.BigInteger

class TransactionSummary(
    val transactionType: TransactionType,
    // Total actually sent, including fee
    val total: BigInteger,
    // Total fee used
    val fee: BigInteger,
    val hash: String,
    val time: Long,
    val confirmations: Int,
    // Address - Amount map
    val inputsMap: Map<String, BigInteger>,
    val outputsMap: Map<String, BigInteger>,
    // Address - xpub map (Fastest way to convert address to xpub)
    val inputsXpubMap: Map<String, String>,
    val outputsXpubMap: Map<String, String>,
    val isDoubleSpend: Boolean = false,
    // Sent to server but not confirmed
    val isPending: Boolean = false
) {
    enum class TransactionType {
        TRANSFERRED,
        RECEIVED,
        SENT,
        BUY,
        SELL,
        SWAP,
        DEPOSIT,
        WITHDRAW,
        INTEREST_EARNED,
        RECURRING_BUY,
        UNKNOWN
    }

    companion object {
        const val ADDRESS_DECODE_ERROR = "[--address_decode_error--]"
    }
}