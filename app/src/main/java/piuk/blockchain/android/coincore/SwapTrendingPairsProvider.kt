package piuk.blockchain.android.coincore

import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.coincore.loader.PAX
import piuk.blockchain.android.identity.Feature
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.ui.dashboard.assetdetails.selectFirstAccount

data class TrendingPair(
    val sourceAccount: CryptoAccount,
    val destinationAccount: CryptoAccount,
    val isSourceFunded: Boolean
) {
    val enabled = isSourceFunded
}

interface TrendingPairsProvider {
    fun getTrendingPairs(): Single<List<TrendingPair>>
}

internal class SwapTrendingPairsProvider(
    private val coincore: Coincore,
    private val identity: UserIdentity
) : TrendingPairsProvider {

    override fun getTrendingPairs(): Single<List<TrendingPair>> =
        identity.isEligibleFor(Feature.SimpleBuy)
            .flatMap { useCustodial ->
                val filter = if (useCustodial) AssetFilter.Custodial else AssetFilter.NonCustodial

                val assetList = makeRequiredAssetSet()
                val accountGroups = assetList.map { asset ->
                    coincore[asset].accountGroup(filter)
                        .toSingle()
                        .onErrorReturn { NullAccountGroup() }
                }

                Single.zip(
                    accountGroups
                ) { groups: Array<Any> ->
                    getAccounts(groups)
                }
            }.map { accountList ->
                val accountMap = accountList.associateBy { it.asset }
                buildPairs(accountMap)
            }.onErrorReturn {
                emptyList()
            }

    private fun makeRequiredAssetSet() =
        DEFAULT_SWAP_PAIRS.map {
            listOf(it.first, it.second, it.first.l2chain)
        }.flatten()
            .filterNotNull()
            .toSet()

    private fun getAccounts(list: Array<Any>): List<CryptoAccount> =
        list.filter { it !is NullAccountGroup }
            .filterIsInstance<AccountGroup>()
            .filter { it.accounts.isNotEmpty() }
            .map { it.selectFirstAccount() }

    private fun buildPairs(accountMap: Map<AssetInfo, CryptoAccount>): List<TrendingPair> =
        DEFAULT_SWAP_PAIRS.mapNotNull { pair ->
            val source = accountMap[pair.first]
            val target = accountMap[pair.second]

            if (source != null && target != null) {
                val chain = pair.first.l2chain?.let {
                    accountMap[it]
                }

                val isFunded = source.isFunded &&
                    (source.isCustodial() || chain == null || chain.isFunded)

                TrendingPair(source, target, isFunded)
            } else {
                null
            }
        }

    companion object {
        private val DEFAULT_SWAP_PAIRS = listOf(
            Pair(CryptoCurrency.BTC, CryptoCurrency.ETHER),
            Pair(CryptoCurrency.BTC, PAX),
            Pair(CryptoCurrency.BTC, CryptoCurrency.XLM),
            Pair(CryptoCurrency.BTC, CryptoCurrency.BCH),
            Pair(CryptoCurrency.ETHER, PAX)
        )
    }
}
