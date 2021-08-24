package piuk.blockchain.android.ui.dashboard.announcements

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.Feature
import com.blockchain.nabu.datamanagers.NabuDataManager
import com.blockchain.nabu.models.responses.nabu.Scope
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.UserIdentity
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.UserCampaignState
import com.blockchain.nabu.service.TierService
import com.blockchain.remoteconfig.RemoteConfig
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.simplebuy.SimpleBuySyncFactory
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class AnnouncementQueries(
    private val nabuToken: NabuToken,
    private val settings: SettingsDataManager,
    private val nabu: NabuDataManager,
    private val tierService: TierService,
    private val sbStateFactory: SimpleBuySyncFactory,
    private val userIdentity: UserIdentity,
    private val coincore: Coincore,
    private val remoteConfig: RemoteConfig,
    private val assetCatalogue: AssetCatalogue
) {
    fun hasFundedFiatWallets(): Single<Boolean> =
        coincore.fiatAssets.accountGroup().toSingle().map {
            it.accounts.any { acc ->
                acc.isFunded
            }
        }

    // Attempt to figure out if KYC/swap etc is allowed based on location...
    fun canKyc(): Single<Boolean> {
        return Singles.zip(
            settings.getSettings()
                .map { it.countryCode }
                .singleOrError(),
            nabu.getCountriesList(Scope.None)
        ).map { (country, list) ->
            list.any { it.code == country && it.isKycAllowed }
        }.onErrorReturn { false }
    }

    // Have we moved past kyc tier 1 - silver?
    fun isKycGoldStartedOrComplete(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.tierInProgressOrCurrentTier == 2 }
            .onErrorReturn { false }
    }

    // Have we been through the Gold KYC process? ie are we Tier2InReview, Tier2Approved or Tier2Failed (cf TierJson)
    fun isGoldComplete(): Single<Boolean> =
        tierService.tiers()
            .map { it.tierCompletedForLevel(KycTierLevel.GOLD) }

    fun isTier1Or2Verified(): Single<Boolean> =
        tierService.tiers().map { it.isVerified() }

    fun isRegistedForStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getUser(token) }
            .map { it.isStxAirdropRegistered }
            .onErrorReturn { false }
    }

    fun isSimplifiedDueDiligenceEligibleAndNotVerified(): Single<Boolean> =
        userIdentity.isEligibleFor(Feature.SimplifiedDueDiligence).flatMap {
            if (!it)
                Single.just(false)
            else
                userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence).map { verified -> verified.not() }
        }

    fun isSimplifiedDueDiligenceVerified(): Single<Boolean> =
        userIdentity.isVerifiedFor(Feature.SimplifiedDueDiligence)

    fun hasReceivedStxAirdrop(): Single<Boolean> {
        return nabuToken.fetchNabuToken()
            .flatMap { token -> nabu.getAirdropCampaignStatus(token) }
            .map { it[blockstackCampaignName]?.userState == UserCampaignState.RewardReceived }
    }

    fun isSimpleBuyKycInProgress(): Single<Boolean> {
        // If we have a local simple buy in progress and it has the kyc unfinished state set
        return Single.defer {
            sbStateFactory.currentState()?.let {
                Single.just(it.kycStartedButNotCompleted)
                    .zipWith(tierService.tiers()) { kycStarted, tier ->
                        kycStarted && !tier.docsSubmittedForGoldTier()
                    }
            } ?: Single.just(false)
        }
    }

    private fun hasSelectedToAddNewCard(): Single<Boolean> =
        Single.defer {
            sbStateFactory.currentState()?.let {
                Single.just(it.selectedPaymentMethod?.id == PaymentMethod.UNDEFINED_CARD_PAYMENT_ID)
            } ?: Single.just(false)
        }

    fun isKycGoldVerifiedAndHasPendingCardToAdd(): Single<Boolean> =
        tierService.tiers().map {
            it.isApprovedFor(KycTierLevel.GOLD)
        }.zipWith(
            hasSelectedToAddNewCard()
        ) { isGold, addNewCard ->
            isGold && addNewCard
        }

    fun getAssetFromCatalogue(): Maybe<AssetInfo> =
        remoteConfig.getRawJson(NEW_ASSET_TICKER).flatMapMaybe { ticker ->
            assetCatalogue.fromNetworkTicker(ticker)?.let { asset ->
                Maybe.just(asset)
            }
                ?: Maybe.empty()
        }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val NEW_ASSET_TICKER = "new_asset_announcement_ticker"
    }
}

private fun KycTiers.docsSubmittedForGoldTier(): Boolean =
    isInitialisedFor(KycTierLevel.GOLD)