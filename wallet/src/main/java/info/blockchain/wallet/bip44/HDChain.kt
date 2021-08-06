package info.blockchain.wallet.bip44

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

/**
 * HDChain.java : a chain in a BIP44 wallet account
 */
class HDChain private constructor(
    private val params: NetworkParameters,
    isReceiveChain: Boolean,
    aKey: DeterministicKey
) {
    private val childKey: DeterministicKey
    // Return BIP44 path for this chain (m / purpose' / coin_type' / account' / chain).
    val path: String
    val xpub: String

    init {
        val chain = if (isReceiveChain) RECEIVE_CHAIN else CHANGE_CHAIN
        childKey = HDKeyDerivation.deriveChildKey(aKey, chain)
        path = childKey.getPathAsString()
        xpub = childKey.serializePubB58(params)
    }

    fun getAddressAt(addressIndex: Int, purpose: Int): HDAddress {
        return HDAddress(params, childKey, addressIndex, purpose)
    }

    companion object {
        private const val RECEIVE_CHAIN = 0
        private const val CHANGE_CHAIN = 1
        const val RECEIVE_CHAIN_DERIVATION_PREFIX = "M/$RECEIVE_CHAIN/"

        fun receiveChain(params: NetworkParameters, rootKey: DeterministicKey): HDChain =
            HDChain(params, true, rootKey)

        fun changeChain(params: NetworkParameters, rootKey: DeterministicKey): HDChain =
            HDChain(params, false, rootKey)
    }
}
