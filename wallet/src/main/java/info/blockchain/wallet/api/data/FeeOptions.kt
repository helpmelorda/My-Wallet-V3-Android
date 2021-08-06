package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class FeeOptions constructor(
    /**
     * Returns a "gasLimit" for Ethereum
     */
    @JsonProperty("gasLimit")
    val gasLimit: Long = 0,

    /**
     * Returns a "regular" fee, which should result in a transaction being included in a block
     * within the next 4-6 hours. The fee is in Satoshis per byte.
     */
    @JsonProperty("regular")
    val regularFee: Long = 0,
    /**
     * Returns a "gasLimit" for Erc20 contract
     */
    @JsonProperty("gasLimitContract")
    val gasLimitContract: Long = 0,

    /**
     * Returns a "priority" fee, which should result in a transaction being included in a block in
     * an hour or so. The fee is in Satoshis per byte.
     */
    @JsonProperty("priority")
    val priorityFee: Long = 0,

    /**
     * Returns a "priority" fee, which should result in a transaction being included in a block in
     * an hour or so. The fee is in Satoshis per byte.
     */
    @JsonProperty("limits")
    val limits: FeeLimits? = null
) {

    companion object {

        /**
         * @return the default FeeOptions for XLM.
         */
        fun defaultForXlm(): FeeOptions {
            return FeeOptions(
                priorityFee = 100,
                regularFee = 100
            )
        }

        /**
         * @return the default FeeOptions for Ethereum.
         */
        fun defaultForEth(): FeeOptions {
            return FeeOptions(
                gasLimit = 21000,
                priorityFee = 23,
                regularFee = 23,
                gasLimitContract = 65000,
                limits = FeeLimits(23, 23)
            )
        }

        /**
         * @return the default FeeOptions for Bitcoin.
         */
        fun defaultForBtc(): FeeOptions {
            return FeeOptions(
                priorityFee = 11,
                regularFee = 5,
                limits = FeeLimits(2, 16)
            )
        }

        fun defaultForBch(): FeeOptions {
            return FeeOptions(
                regularFee = 4,
                priorityFee = 4
            )
        }

        fun defaultForErc20(): FeeOptions = defaultForEth()
    }
}
