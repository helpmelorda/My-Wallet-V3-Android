package info.blockchain.wallet.ethereum

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import java.util.HashMap

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)

class Erc20TokenData {

    @JsonProperty("label")
    var label: String = ""

    @JsonProperty("contract")
    var contractAddress: String = ""
        private set

    @JsonProperty("has_seen")
    var hasSeen: Boolean = false

    @JsonProperty("tx_notes")
    val txNotes: HashMap<String, String> = HashMap()

    fun putTxNote(txHash: String, txNote: String) {
        txNotes[txHash] = txNote
    }

    fun removeTxNote(txHash: String) {
        txNotes.remove(txHash)
    }

    fun clearTxNotes() {
        txNotes.clear()
    }

    fun hasLabelAndAddressStored(): Boolean =
        contractAddress.isNotBlank() && label.isNotBlank()

    companion object {
        fun createTokenData(asset: AssetInfo, label: String): Erc20TokenData {
            require(asset.l2chain == CryptoCurrency.ETHER)
            require(asset.l2identifier != null)

            return Erc20TokenData().apply {
                contractAddress = asset.l2identifier!!
                this.label = label
            }
        }
    }
}