package info.blockchain.wallet.multiaddress

import com.blockchain.api.ApiException
import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.MultiAddress
import info.blockchain.wallet.payload.data.AddressLabel
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.allAddresses
import retrofit2.Call

abstract class MultiAddressFactory(
    internal val bitcoinApi: NonCustodialBitcoinService
) {
    private val nextReceiveAddressMap: HashMap<String, Int> = HashMap()
    private val nextChangeAddressMap: HashMap<String, Int> = HashMap()

    // Field for testing if address belongs to us - Quicker than derivation
    private val addressToXpubMap: HashMap<String, String> = HashMap()

    fun getXpubFromAddress(address: String): String? {
        return addressToXpubMap[address]
    }

    /**
     * @param all A list of all xpubs and legacy addresses whose transactions are to
     * be retrieved from API.
     * @param activeImported (Hacky! Needs a rethink) Only set this when fetching a transaction list
     * for imported addresses, otherwise set as Null.
     * A list of all active legacy addresses. Used for 'Imported address' transaction list.
     * @param onlyShow Xpub or legacy address. Used to fetch transaction only relating to this
     * address. Set as Null for a consolidated list like 'All Accounts' or 'Imported'.
     * @param limit Maximum amount of transactions fetched
     * @param offset Page offset
     */

    fun getAccountTransactions(
        all: List<XPubs>,
        onlyShow: List<String>?,
        limit: Int,
        offset: Int,
        startingBlockHeight: Int
    ): List<TransactionSummary> {

        val multiAddress = getMultiAddress(all, onlyShow, limit, offset)
        return if (multiAddress?.txs == null) {
            emptyList()
        } else {
            summarize(
                all,
                multiAddress,
                startingBlockHeight
            )
        }
    }

    private fun getMultiAddress(
        xpubs: List<XPubs>,
        onlyShow: List<String>?,
        limit: Int,
        offset: Int
    ): MultiAddress? {

        val call = getMultiAddress(xpubs, limit, offset, onlyShow)
        val response = call.execute()

        return if (response.isSuccessful) {
            response.body()
        } else {
            throw ApiException(response.errorBody()!!.string())
        }
    }

    protected abstract fun getMultiAddress(
        xpubs: List<XPubs>,
        limit: Int,
        offset: Int,
        context: List<String>?
    ): Call<MultiAddress>

    fun getNextChangeAddressIndex(xpub: String): Int =
        if (nextChangeAddressMap.containsKey(xpub)) {
            nextChangeAddressMap[xpub]!!
        } else {
            0
        }

    fun getNextReceiveAddressIndex(xpub: String, reservedAddresses: List<AddressLabel>): Int {
        if (!nextReceiveAddressMap.containsKey(xpub)) {
            return 0
        }

        var receiveIndex: Int? = nextReceiveAddressMap[xpub]
        // Skip reserved addresses
        for ((index) in reservedAddresses) {
            if (index == receiveIndex) {
                receiveIndex++
            }
        }

        return receiveIndex!!
    }

    fun isOwnHDAddress(address: String): Boolean {
        return addressToXpubMap.containsKey(address)
    }

    @Deprecated("Use the XPub version")
    fun incrementNextReceiveAddress(xpub: XPub, reservedAddresses: List<AddressLabel>) {
        var receiveIndex = getNextReceiveAddressIndex(xpub.address, reservedAddresses)
        receiveIndex++

        nextReceiveAddressMap[xpub.address] = receiveIndex
    }

    @Deprecated("Use the XPub version")
    fun incrementNextReceiveAddress(xpub: String, reservedAddresses: List<AddressLabel>) {
        val receiveIndex = getNextReceiveAddressIndex(xpub, reservedAddresses) + 1
        nextReceiveAddressMap[xpub] = receiveIndex
    }

    fun incrementNextChangeAddress(xpub: String) {
        val index = getNextChangeAddressIndex(xpub) + 1
        nextChangeAddressMap[xpub] = index
    }

    private fun summarize(
        xpubs: List<XPubs>,
        multiAddress: MultiAddress,
        startingBlockHeight: Int
    ): List<TransactionSummary> {
        val ownAddresses = xpubs.allAddresses().toMutableList()
        val summaryList = ArrayList<TransactionSummary>()

        // Set next address indexes
        for (address in multiAddress.addresses) {
            nextReceiveAddressMap[address.address] = address.accountIndex
            nextChangeAddressMap[address.address] = address.changeIndex
        }
        val latestBlock = multiAddress.info.latestBlock.height.toInt()
        multiAddress.txs.forEach { tx ->
            tx.toTransactionSummary(
                ownAddresses = ownAddresses,
                startingBlockHeight = startingBlockHeight,
                latestBlock = latestBlock
            )?.let { txSummary ->
                addressToXpubMap.putAll(txSummary.inputsXpubMap)
                addressToXpubMap.putAll(txSummary.outputsXpubMap)
                summaryList.add(txSummary)
            }
        }
        return summaryList
    }
}