package piuk.blockchain.android.coincore.loader

import info.blockchain.balance.AssetCategory
import info.blockchain.balance.CryptoCurrency

internal object ALGO : CryptoCurrency(
    ticker = "ALGO",
    name = "Algorand",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 6,
    requiredConfirmations = 12,
    startDate = 1560985200L, // 2019-06-20 00:00:00 UTC
    colour = "#000000",
    logo = "file:///android_asset/logo/algorand/logo.png",
    txExplorerUrlBase = "https://algoexplorer.io/tx/"
)

internal object DOT : CryptoCurrency(
    ticker = "DOT",
    name = "Polkadot",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 10,
    requiredConfirmations = 12,
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#E6007A",
    logo = "file:///android_asset/logo/polkadot/logo.png",
    txExplorerUrlBase = "https://polkascan.io/polkadot/tx/"
)

internal object DOGE : CryptoCurrency(
    ticker = "DOGE",
    name = "Dogecoin",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 8,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#C2A633",
    logo = "file:///android_asset/logo/dogecoin/logo.png"
)

internal object CLOUT : CryptoCurrency(
    ticker = "CLOUT",
    name = "BitClout",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 9,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#000000",
    logo = "file:///android_asset/logo/bitclout/logo.png"
)

internal object LTC : CryptoCurrency(
    ticker = "LTC",
    name = "Litecoin",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 8,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#BFBBBB",
    logo = "file:///android_asset/logo/litecoin/logo.png"
)

internal object ETC : CryptoCurrency(
    ticker = "ETC",
    name = "Ethereum Classic",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 8,
    requiredConfirmations = 18, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#33FF99",
    logo = "file:///android_asset/logo/ethereum_classic/logo.png"
)

/*
Not supported in this pass.
internal object ZEN : CryptoCurrency(
    ticker = "ZEN",
    name = "Horizen",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 8, // TODO: This is unknown ATM - get the real value
    requiredConfirmations = 12, // Temp - TODO Get real value
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#041742",
    logo = "file:///android_asset/logo/horizen/logo.png"
)
*/

internal object XTZ : CryptoCurrency(
    ticker = "XTZ",
    name = "Tezos",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 6,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#2C7DF7",
    logo = "file:///android_asset/logo/tezos/logo.png"
)

internal object STX : CryptoCurrency(
    ticker = "STX",
    name = "Stacks",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 6,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#211F6D",
    logo = "file:///android_asset/logo/blockstack/logo.png"
)

internal object MOB : CryptoCurrency(
    ticker = "MOB",
    name = "Mobile Coin",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 12,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#243855",
    logo = "file:///android_asset/logo/mobilecoin/logo.png"
)

internal object THETA : CryptoCurrency(
    ticker = "THETA",
    name = "Theta Network",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 18,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#2AB8E6",
    logo = "file:///android_asset/logo/theta/logo.png"
)

internal object NEAR : CryptoCurrency(
    ticker = "NEAR",
    name = "NEAR Protocol",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 24,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#000000",
    logo = "file:///android_asset/logo/near_protocol/logo.png"
)

internal object EOS : CryptoCurrency(
    ticker = "EOS",
    name = "EOS",
    categories = setOf(AssetCategory.CUSTODIAL),
    precisionDp = 4,
    requiredConfirmations = 12, // Temp - TODO Get real value. Not needed for custodial-only
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#000000",
    logo = "file:///android_asset/logo/eos/logo.png"
)

// ERC20 custodial assets. Will need integrating with dynamic erc20
internal object OGN : CryptoCurrency(
    ticker = "OGN",
    name = "Origin Token (OGN)",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x8207c1FfC5B6804F6024322CcF34F29c3541Ae26",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#1A82FF",
    logo = "file:///android_asset/logo/origin/logo.png"
)

internal object ENJ : CryptoCurrency(
    ticker = "ENJ",
    name = "Enjin Coin",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#624DBF",
    logo = "file:///android_asset/logo/enjin/logo.png"
)

internal object COMP : CryptoCurrency(
    ticker = "COMP",
    name = "Compound",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0xc00e94Cb662C3520282E6f5717214004A7f26888",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#00D395",
    logo = "file:///android_asset/logo/compound/logo.png"
)

internal object LINK : CryptoCurrency(
    ticker = "LINK",
    name = "Chainlink",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x514910771AF9Ca656af840dff83E8264EcF986CA",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#2A5ADA",
    logo = "file:///android_asset/logo/chainlink/logo.png"
)

internal object TBTC : CryptoCurrency(
    ticker = "TBTC",
    name = "tBTC",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x8dAEBADE922dF735c38C80C7eBD708Af50815fAa",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = ETHER.colour,
    logo = "file:///android_asset/logo/tbtc/logo.png"
)

internal object WBTC : CryptoCurrency(
    ticker = "WBTC",
    name = "Wrapped Bitcoin",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 8,
    l2chain = ETHER,
    l2identifier = "0x2260FAC5E5542a773Aa44fBCfeDf7C193bc2C599",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = ETHER.colour,
    logo = "file:///android_asset/logo/wbtc/logo.png"
)

internal object SNX : CryptoCurrency(
    ticker = "SNX",
    name = "Synthetix Network Token",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0xC011a73ee8576Fb46F5E1c5751cA3B9Fe0af2a6F",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = ETHER.colour,
    logo = "file:///android_asset/logo/synthetix/logo.png"
)

internal object SUSHI : CryptoCurrency(
    ticker = "SUSHI",
    name = "Sushi",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x6B3595068778DD592e39A122f4f5a5cF09C90fE2",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = ETHER.colour,
    logo = "file:///android_asset/logo/sushi/logo.png"
)

internal object ZRX : CryptoCurrency(
    ticker = "ZRX",
    name = "ZRX",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0xE41d2489571d322189246DaFA5ebDe1F4699F498",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = ETHER.colour,
    logo = "file:///android_asset/logo/zrx/logo.png"
)

internal object USDC : CryptoCurrency(
    ticker = "USDC",
    name = "USD Coin",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 6,
    l2chain = ETHER,
    l2identifier = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
    requiredConfirmations = 12, // Same as ETHER
    colour = "#2775CA",
    logo = "file:///android_asset/logo/usdc/logo.png"
)

internal object UNI : CryptoCurrency(
    ticker = "UNI",
    name = "Uniswap",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#FF007A",
    logo = "file:///android_asset/logo/uniswap/logo.png"
)

internal object DAI : CryptoCurrency(
    ticker = "DAI",
    name = "Multi-collateral DAI",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x6B175474E89094C44Da98b954EedeAC495271d0F",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#F5AC37",
    logo = "file:///android_asset/logo/dai/logo.png"
)

internal object BAT : CryptoCurrency(
    ticker = "BAT",
    name = "Basic Attention Token",
    categories = setOf(AssetCategory.CUSTODIAL, AssetCategory.NON_CUSTODIAL),
    precisionDp = 18,
    l2chain = ETHER,
    l2identifier = "0x0D8775F648430679A709E98d2b0Cb6250d2887EF",
    requiredConfirmations = 12, // Same as ETHER
    startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
    colour = "#FF4724",
    logo = "file:///android_asset/logo/bat/logo.png"
)
