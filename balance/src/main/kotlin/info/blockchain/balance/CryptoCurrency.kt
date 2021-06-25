package info.blockchain.balance

interface AssetInfo {
    val ticker: String
    val name: String
    // token price start times in epoch-seconds. null if charting not supported
    val startDate: Long?
    // max decimal places; ie the quanta of this asset
    val precisionDp: Int
    val requiredConfirmations: Int // Not sure this should be here TODO: Review this
    // If non-null, then this is an l2 asset, and this contains the ticker of the chain on which this is implemented?
    val l2chain: AssetInfo?
    // If non-null, then this an l2 asset and this is the id on the l1 chain. Ie contract address for erc20 assets
    val l2identifier: String?
    // For now, while I get this building
    val isCustodialOnly: Boolean
    // Resources
    val colour: String
    val logo: String?
}

interface AssetCatalogue {
    fun fromNetworkTicker(symbol: String): AssetInfo?
    fun supportedCryptoAssets(): List<AssetInfo>
    fun supportedL2Assets(chain: AssetInfo): List<AssetInfo>

    // Might maker this more flexible later, but now we need:
    fun supportedCustodialAssets(): List<AssetInfo>
}

open class CryptoCurrency(
    override val ticker: String,
    override val name: String,
    override val precisionDp: Int,
    override val startDate: Long? = null, // token price start times in epoch-seconds. null if charting not supported
    override val requiredConfirmations: Int,
    override val l2chain: AssetInfo? = null,
    override val l2identifier: String? = null,
    override val isCustodialOnly: Boolean = false,
    override val colour: String,
    override val logo: String? = null
) : AssetInfo {

    object BTC : CryptoCurrency(
        ticker = "BTC",
        name = "Bitcoin",
        precisionDp = 8,
        requiredConfirmations = 3,
        startDate = 1282089600L, // 2010-08-18 00:00:00 UTC
        colour = "#FF9B22"
    )

    object ETHER : CryptoCurrency(
        ticker = "ETH",
        name = "Ether",
        precisionDp = 18,
        requiredConfirmations = 12,
        startDate = 1438992000L, // 2015-08-08 00:00:00 UTC
        colour = "#473BCB"
    )

    object BCH : CryptoCurrency(
        ticker = "BCH",
        name = "Bitcoin Cash",
        precisionDp = 8,
        requiredConfirmations = 3,
        startDate = 1500854400L, // 2017-07-24 00:00:00 UTC
        colour = "#8DC351"
    )

    object XLM : CryptoCurrency(
        ticker = "XLM",
        name = "Stellar",
        precisionDp = 7,
        requiredConfirmations = 1,
        startDate = 1409875200L, // 2014-09-04 00:00:00 UTC
        colour = "#000000"
    )

    object ALGO : CryptoCurrency(
        ticker = "ALGO",
        name = "Algorand",
        precisionDp = 6,
        requiredConfirmations = 12,
        startDate = 1560985200L, // 2019-06-20 00:00:00 UTC
        isCustodialOnly = true,
        colour = "#000000"
    )

    object DOT : CryptoCurrency(
        ticker = "DOT",
        name = "Polkadot",
        precisionDp = 10,
        requiredConfirmations = 12,
        startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
        isCustodialOnly = true,
        colour = "#E6007A"
    )

    object STX : CryptoCurrency(
        ticker = "STX",
        name = "Stacks",
        precisionDp = 7,
        requiredConfirmations = 12,
        startDate = 0,
        colour = "#000000"
    )
}

fun AssetInfo.isErc20() =
    l2chain?.equals(CryptoCurrency.ETHER) == true