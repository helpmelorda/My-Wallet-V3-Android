package piuk.blockchain.android.ui.airdrops

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.AirdropStatus
import com.blockchain.nabu.models.responses.nabu.AirdropStatusList
import com.blockchain.nabu.models.responses.nabu.CampaignState
import com.blockchain.nabu.models.responses.nabu.CampaignTransactionState
import com.blockchain.nabu.models.responses.nabu.UserCampaignState
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.campaign.sunriverCampaignName
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import timber.log.Timber
import java.lang.IllegalStateException
import java.util.Date

interface AirdropCentreView : MvpView {
    fun renderList(statusList: List<Airdrop>)
    fun renderListUnavailable()
}

class AirdropCentrePresenter(
    private val nabuToken: NabuToken,
    private val nabu: NabuDataManager,
    private val assetCatalogue: AssetCatalogue,
    private val crashLogger: CrashLogger
) : MvpPresenter<AirdropCentreView>() {

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = false

    override fun onViewAttached() {
        fetchAirdropStatus()
    }

    override fun onViewDetached() { }

    private fun fetchAirdropStatus() {
        compositeDisposable += nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getAirdropCampaignStatus(token) }
            .map { list -> remapStateList(list) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { renderUi(it) },
                onError = {
                    crashLogger.logException(it)
                    view?.renderListUnavailable()
                }
            )
    }

    private fun remapStateList(statusList: AirdropStatusList): List<Airdrop> =
        statusList.airdropList.mapNotNull { transformAirdropStatus(it) }.toList()

    private fun transformAirdropStatus(item: AirdropStatus): Airdrop? {
        val name = item.campaignName
        val asset = when (name) {
            blockstackCampaignName -> AIRDROP_STX
            sunriverCampaignName -> CryptoCurrency.XLM
            else -> return null
        }

        val status = parseState(item)
        val date = parseDate(item)
        val (amountFiat, amountCrypto) = parseAmount(item)

        return Airdrop(
            name,
            asset,
            status,
            amountFiat,
            amountCrypto,
            date
        )
    }

    private fun parseAmount(item: AirdropStatus): Pair<FiatValue?, CryptoValue?> {

        val tx = item.txResponseList
            .firstOrNull {
                it.transactionState == CampaignTransactionState.FinishedWithdrawal
            }

        return tx?.let {
            Pair(
                FiatValue.fromMinor(
                    tx.fiatCurrency,
                    tx.fiatValue
                ),
                CryptoValue.fromMinor(
                    parseAsset(tx.withdrawalCurrency),
                    tx.withdrawalQuantity.toBigDecimal()
                )
            )
        } ?: Pair(null, null)
    }

    private fun parseAsset(ticker: String): AssetInfo {
        val asset = assetCatalogue.fromNetworkTicker(ticker)
        // STX is not, currently, a supported asset so we'll have to check that manually here for now.
        // When we get full-dynamic assets _and_ it's supported, we can take this out TODO
        return when {
            asset != null -> asset
            ticker.compareTo(AIRDROP_STX.ticker, ignoreCase = true) == 0 -> AIRDROP_STX
            else -> throw IllegalStateException("Unknown crypto currency: $ticker")
        }
    }

    private fun parseState(item: AirdropStatus): AirdropState =
        if (item.campaignState == CampaignState.Ended) {
            when (item.userState) {
                UserCampaignState.RewardReceived -> AirdropState.RECEIVED
                else -> AirdropState.EXPIRED
            }
        } else {
            AirdropState.UNKNOWN
        }

    private fun parseDate(item: AirdropStatus): Date? {
        with(item) {
            return if (txResponseList.isNullOrEmpty()) {
                when (campaignName) {
                    blockstackCampaignName ->
                        if (userState == UserCampaignState.RewardReceived) {
                            updatedAt
                        } else {
                            campaignEndDate
                        }
                    sunriverCampaignName -> campaignEndDate
                    else -> null
                }
            } else {
                txResponseList.maxByOrNull {
                    it.withdrawalAt
                }?.withdrawalAt ?: throw IllegalStateException("Can't happen")
            }
        }
    }

    private fun renderUi(statusList: List<Airdrop>) {
        Timber.d("Got status!")
        view?.renderList(statusList)
    }

    // STUB Asset; only used in the airdrop screen
    @Suppress("ClassName")
    private object AIRDROP_STX : CryptoCurrency(
        ticker = "STX",
        name = "Stacks",
        categories = emptySet(),
        precisionDp = 6,
        requiredConfirmations = 12,
        startDate = 1615831200L, // 2021-03-15 00:00:00 UTC
        colour = "#211F6D",
        logo = "file:///android_asset/logo/blockstack/logo.png"
    )
}

enum class AirdropState {
    UNKNOWN,
    REGISTERED,
    EXPIRED,
    PENDING,
    RECEIVED
}

data class Airdrop(
    val name: String,
    val asset: AssetInfo,
    val status: AirdropState,
    val amountFiat: FiatValue?,
    val amountCrypto: CryptoValue?,
    val date: Date?
) {
    val isActive: Boolean = (status == AirdropState.PENDING || status == AirdropState.REGISTERED)
}
