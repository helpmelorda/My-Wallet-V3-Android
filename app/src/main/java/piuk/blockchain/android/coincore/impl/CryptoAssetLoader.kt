package piuk.blockchain.android.coincore.impl

import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.erc20.Erc20Asset
import piuk.blockchain.android.identity.UserIdentity
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateService
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

// TODO this will change to support both fiat and crypto, when we have a common interface/class for both
interface AssetLoader {
    fun loadCryptoAssets(): Single<List<CryptoAsset>>
}

class CryptoAssetLoader(
    private val fixedAssets: List<CryptoAsset>,
    private val assetCatalogue: AssetCatalogue,
    private val payloadManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val feeDataManager: FeeDataManager,
    private val walletPreferences: WalletStatus,
    private val custodialManager: CustodialWalletManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val historicRates: ExchangeRateService,
    private val currencyPrefs: CurrencyPrefs,
    private val labels: DefaultLabels,
    private val pitLinking: PitLinking,
    private val crashLogger: CrashLogger,
    private val identity: UserIdentity,
    private val features: InternalFeatureFlagApi
) : AssetLoader {

    // When building the asset list, make sure that any l1 assest are earlier in the list than any l2 for that
    // chain -  ETH before ERC20 - to ensure that asset init works correctly.
    // Since, ETH is a fixed asset and erc20 are appended, we don't need to do anything special at this time
    override fun loadCryptoAssets(): Single<List<CryptoAsset>> =
        Single.fromCallable {
            fixedAssets + loadErc20Assets()
        }

    private fun loadErc20Assets(): List<CryptoAsset> {
        val erc20 = assetCatalogue.supportedL2Assets(CryptoCurrency.ETHER)

        return erc20.map {
            Erc20Asset(
                asset = it,
                payloadManager = payloadManager,
                ethDataManager = ethDataManager,
                feeDataManager = feeDataManager,
                exchangeRates = exchangeRates,
                historicRates = historicRates,
                currencyPrefs = currencyPrefs,
                custodialManager = custodialManager,
                crashLogger = crashLogger,
                labels = labels,
                pitLinking = pitLinking,
                walletPreferences = walletPreferences,
                identity = identity,
                features = features
            )
        }
    }
}
