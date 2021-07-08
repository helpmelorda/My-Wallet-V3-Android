package piuk.blockchain.android.coincore.impl

import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency

internal object PAX : CryptoCurrency(
    ticker = "PAX",
    name = "Paxos Standard",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x8E870D67F660D95d5be530380D0eC0bd388289E1",
    requiredConfirmations = 12, // Same as ETHER
    colour = "#00522C",
    logo = "file:///android_asset/logo/paxos/logo.png",
    txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
)

internal object USDT : CryptoCurrency(
    ticker = "USDT",
    name = "Tether",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 6,
    l2chain = ETHER,
    l2identifier = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
    requiredConfirmations = 12, // Same as ETHER
    colour = "#26A17B",
    logo = "file:///android_asset/logo/tether/logo.png",
    txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"

)

internal object WDGLD : CryptoCurrency(
    ticker = "WDGLD",
    name = "Wrapped-DGLD",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 8,
    l2chain = ETHER,
    l2identifier = "0x123151402076fc819b7564510989e475c9cd93ca",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1576108800L, // 2019-12-12 00:00:00 UTC
    colour = "#A39424",
    logo = "file:///android_asset/logo/wdgld/logo.png",
    txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
)

internal object AAVE : CryptoCurrency(
    ticker = "AAVE",
    name = "Aave",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#2EBAC6",
    logo = "file:///android_asset/logo/aave/logo.png",
    txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
)

internal object YFI : CryptoCurrency(
    ticker = "YFI",
    name = "YFI", // TODO Check name
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x0bc529c00C6401aEF6D220BE8C6Ea1667F6Ad93e",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // Same as AAVE
    colour = "#0074FA",
    logo = "file:///android_asset/logo/yearn_finance/logo.png",
    txExplorerUrlBase = "https://www.blockchain.com/eth/tx/"
)
