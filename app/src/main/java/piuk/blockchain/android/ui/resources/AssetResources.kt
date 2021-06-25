package piuk.blockchain.android.ui.resources

import android.content.res.Resources
import android.graphics.Color
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.R
import java.util.Locale

interface AssetResources {

    @ColorInt
    fun assetColor(asset: AssetInfo): Int

    fun loadAssetIcon(imageView: ImageView, asset: AssetInfo)

    @DrawableRes
    fun fiatCurrencyIcon(currency: String): Int

    fun makeBlockExplorerUrl(asset: AssetInfo, transactionHash: String): String
}

internal class AssetResourcesImpl(val resources: Resources) : AssetResources {

    override fun assetColor(asset: AssetInfo): Int =
        Color.parseColor(asset.colour)

    override fun loadAssetIcon(imageView: ImageView, asset: AssetInfo) {
        asset.logo?.let {
            Glide.with(imageView.context)
                .load(it)
                // TODO: Load defauly placeholder/failover asset logo
                .into(imageView)
        } ?: loadResourceAsset(imageView, asset)
    }

    @Deprecated("All asset logs will load over the network")
    private fun loadResourceAsset(imageView: ImageView, asset: AssetInfo) {
        Glide.with(imageView.context)
            .load("")
            .apply(
                RequestOptions().placeholder(assetIcon(asset))
            ).into(imageView)
    }

    private fun assetIcon(asset: AssetInfo): Int =
        when (asset.ticker.toUpperCase(Locale.ROOT)) {
            CryptoCurrency.BTC.ticker -> R.drawable.vector_bitcoin_colored
            CryptoCurrency.ETHER.ticker -> R.drawable.vector_eth_colored
            CryptoCurrency.BCH.ticker -> R.drawable.vector_bitcoin_cash_colored
            CryptoCurrency.XLM.ticker -> R.drawable.vector_xlm_colored
            CryptoCurrency.DOT.ticker -> R.drawable.vector_dot_colored
            CryptoCurrency.ALGO.ticker -> R.drawable.vector_algo_colored
            CryptoCurrency.STX.ticker -> R.drawable.ic_logo_stx
            // TODO: Get WDGLD logo uri when it's supported in the erc20 asset list. FIXME
            "WDGLD" -> R.drawable.vector_dgld_colored
            else -> throw NotImplementedError("${asset.ticker} Not implemented")
        }

    override fun makeBlockExplorerUrl(asset: AssetInfo, transactionHash: String): String =
        when (asset.ticker.toUpperCase(Locale.ROOT)) {
            CryptoCurrency.BTC.ticker -> "https://www.blockchain.com/btc/tx/"
            CryptoCurrency.BCH.ticker -> "https://www.blockchain.com/bch/tx/"
            CryptoCurrency.XLM.ticker -> "https://stellarchain.io/tx/"
            CryptoCurrency.ALGO.ticker -> "https://algoexplorer.io/tx/"
            CryptoCurrency.DOT.ticker -> "https://polkascan.io/polkadot/tx/"
            CryptoCurrency.ETHER.ticker,
            "PAX",
            "USDT",
            "WDGLD",
            "AAVE",
            "YFI" -> "https://www.blockchain.com/eth/tx/"
            else -> throw NotImplementedError("${asset.ticker} Not implemented")
        } + transactionHash

    override fun fiatCurrencyIcon(currency: String): Int =
        when (currency) {
            "EUR" -> R.drawable.ic_funds_euro
            "GBP" -> R.drawable.ic_funds_gbp
            else -> R.drawable.ic_funds_usd
        }
}
