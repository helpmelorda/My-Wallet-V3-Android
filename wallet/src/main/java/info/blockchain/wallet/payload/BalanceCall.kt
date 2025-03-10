package info.blockchain.wallet.payload

import info.blockchain.balance.AssetInfo
import com.blockchain.api.services.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.BalanceResponseDto
import info.blockchain.wallet.exceptions.ServerConnectionException
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import info.blockchain.wallet.payload.model.Balance
import info.blockchain.wallet.payload.model.toBalanceMap
import retrofit2.Response
import java.math.BigInteger
import java.util.Locale

class BalanceCall(
    private val bitcoinApi: NonCustodialBitcoinService,
    private val asset: AssetInfo
) : BalanceQuery {

    override fun getBalancesForXPubs(xpubs: List<XPubs>, legacyImported: List<String>): Map<String, BigInteger> =
        getBalanceOfXpubs(
            legacyAddresses = xpubs.legacyXpubAddresses() + legacyImported,
            segwitAddresses = xpubs.segwitXpubAddresses()
        ).execute()
            .let { buildBalanceMap(it) }

    override fun getBalancesForAddresses(
        addresses: List<String>,
        legacyImported: List<String>
    ): Map<String, BigInteger> =
        getBalanceOfAddresses(addresses + legacyImported)
            .execute()
            .let { buildBalanceMap(it) }

    private fun getBalanceOfAddresses(addresses: List<String>) =
        bitcoinApi.getBalance(
            asset.ticker.toLowerCase(Locale.ROOT),
            addresses,
            emptyList(),
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable
        )

    private fun getBalanceOfXpubs(legacyAddresses: List<String>, segwitAddresses: List<String>) =
        bitcoinApi.getBalance(
            asset.ticker.toLowerCase(Locale.ROOT),
            legacyAddresses,
            segwitAddresses,
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable
        )

    private fun buildBalanceMap(response: Response<BalanceResponseDto>): Map<String, BigInteger> {
        if (!response.isSuccessful) {
            throw ServerConnectionException(response.errorBody()?.string() ?: "Unknown, no error body")
        }
        return response.body()?.toBalanceMap()?.finalBalanceMap() ?: throw Exception("No balances returned")
    }
}

private fun <K> Map<K, Balance>.finalBalanceMap() =
    map { (k, v) -> k to v.finalBalance }.toMap()
