package piuk.blockchain.android.ui.transfer

import io.reactivex.rxjava3.core.Single
import com.blockchain.preferences.DashboardPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.SingleAccount

interface AccountsSorting {
    fun sorter(): AccountsSorter
}

typealias AccountsSorter = (List<SingleAccount>) -> Single<List<SingleAccount>>

class DashboardAccountsSorting(
    private val dashboardPrefs: DashboardPrefs,
    private val assetCatalogue: AssetCatalogue
) : AccountsSorting {

    override fun sorter(): AccountsSorter = { list ->
        Single.fromCallable { getOrdering() }
            .map { orderedAssets ->
                val sortedList = list.sortedWith(
                    compareBy(
                        {
                            (it as? CryptoAccount)?.let { cryptoAccount ->
                                orderedAssets.indexOf(cryptoAccount.asset)
                            } ?: 0
                        },
                        { it !is NonCustodialAccount },
                        { !it.isDefault }
                    )
                )
                sortedList
            }
        }

    private fun getOrdering(): List<AssetInfo> =
        dashboardPrefs.dashboardAssetOrder
            .takeIf { it.isNotEmpty() }?.let {
                it.mapNotNull { ticker -> assetCatalogue.fromNetworkTicker(ticker) }
            } ?: assetCatalogue.supportedCryptoAssets.sortedBy { it.ticker }
}