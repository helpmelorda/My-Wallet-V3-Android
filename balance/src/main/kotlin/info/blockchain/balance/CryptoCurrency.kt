package info.blockchain.balance

enum class AssetCategory {
    CUSTODIAL,
    NON_CUSTODIAL
}

interface AssetInfo {
    val ticker: String
    val name: String
    val categories: Set<AssetCategory>
    // token price start times in epoch-seconds. null if charting not supported
    val startDate: Long?
    // max decimal places; ie the quanta of this asset
    val precisionDp: Int
    val requiredConfirmations: Int
    // If non-null, then this is an l2 asset, and this contains the ticker of the chain on which this is implemented?
    val l2chain: AssetInfo?
    // If non-null, then this an l2 asset and this is the id on the l1 chain. Ie contract address for erc20 assets
    val l2identifier: String?
    // Resources
    val colour: String
    val logo: String
    val txExplorerUrlBase: String?
}

val AssetInfo.isCustodialOnly: Boolean
    get() = categories.size == 1 && categories.contains(AssetCategory.CUSTODIAL)

val AssetInfo.isCustodial: Boolean
    get() = categories.contains(AssetCategory.CUSTODIAL)

val AssetInfo.isNonCustodialOnly: Boolean
    get() = categories.size == 1 && categories.contains(AssetCategory.NON_CUSTODIAL)

val AssetInfo.isNonCustodial: Boolean
    get() = categories.contains(AssetCategory.NON_CUSTODIAL)

interface AssetCatalogue {
    val supportedCryptoAssets: List<AssetInfo>
    val supportedCustodialAssets: List<AssetInfo>

    fun fromNetworkTicker(symbol: String): AssetInfo?
    fun supportedL2Assets(chain: AssetInfo): List<AssetInfo>
}

open class CryptoCurrency(
    override val ticker: String,
    override val name: String,
    override val categories: Set<AssetCategory>,
    override val precisionDp: Int,
    override val startDate: Long? = null, // token price start times in epoch-seconds. null if charting not supported
    override val requiredConfirmations: Int,
    override val l2chain: AssetInfo? = null,
    override val l2identifier: String? = null,
    override val colour: String,
    override val logo: String = "",
    override val txExplorerUrlBase: String? = null
) : AssetInfo {

    object BTC : CryptoCurrency(
        ticker = "BTC",
        name = "Bitcoin",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        startDate = 1282089600L, // 2010-08-18 00:00:00 UTC
        colour = "#FF9B22",
        logo = "file:///android_asset/logo/bitcoin/logo.png",
        txExplorerUrlBase = "https://www.blockchain.com/btc/tx/"
    )

    object ETHER : CryptoCurrency(
        ticker = "ETH",
        name = "Ether",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 18,
        requiredConfirmations = 12,
        startDate = 1438992000L, // 2015-08-08 00:00:00 UTC
        colour = "#473BCB",
        logo = "file:///android_asset/logo/ethereum/logo.png",
        txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
    )

    object BCH : CryptoCurrency(
        ticker = "BCH",
        name = "Bitcoin Cash",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 8,
        requiredConfirmations = 3,
        startDate = 1500854400L, // 2017-07-24 00:00:00 UTC
        colour = "#8DC351",
        logo = "file:///android_asset/logo/bitcoin_cash/logo.png",
        txExplorerUrlBase = "https://www.blockchain.com/bch/tx/"
    )

    object XLM : CryptoCurrency(
        ticker = "XLM",
        name = "Stellar",
        categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
        precisionDp = 7,
        requiredConfirmations = 1,
        startDate = 1409875200L, // 2014-09-04 00:00:00 UTC
        colour = "#000000",
        logo = "file:///android_asset/logo/stellar/logo.png",
        txExplorerUrlBase = "https://stellarchain.io/tx/"
    )
}

fun AssetInfo.isErc20() =
    l2chain?.equals(CryptoCurrency.ETHER) == true