package info.blockchain.wallet.multiaddress

import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.MultiAddress
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import retrofit2.Call

class MultiAddressFactoryBtc(bitcoinApi: NonCustodialBitcoinService)
    : MultiAddressFactory(bitcoinApi) {

    override fun getMultiAddress(
        xpubs: List<XPubs>,
        limit: Int,
        offset: Int,
        context: List<String>?
    ): Call<MultiAddress> {
        val r = bitcoinApi.getMultiAddress(
            NonCustodialBitcoinService.BITCOIN,
            xpubs.legacyXpubAddresses(),
            xpubs.segwitXpubAddresses(),
            context?.joinToString("|"),
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable,
            limit,
            offset
        )
        return r
    }
}
