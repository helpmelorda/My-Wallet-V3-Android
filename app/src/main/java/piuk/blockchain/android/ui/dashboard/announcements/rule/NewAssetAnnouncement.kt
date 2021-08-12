package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class NewAssetAnnouncement(
    private val announcementQueries: AnnouncementQueries,
    dismissRecorder: DismissRecorder
) : AnnouncementRule(dismissRecorder) {
    private var newAsset: AssetInfo? = null

    override val dismissKey: String
        get() = DISMISS_KEY.plus(newAsset?.ticker.orEmpty())

    override val name: String
        get() = "new_asset"

    override fun shouldShow(): Single<Boolean> =
        announcementQueries.getAssetFromCatalogue()
            .doOnSuccess {
                newAsset = it
            }
            .toSingle()
            .map {
                !dismissEntry.isDismissed
            }.onErrorReturn { false }

    override fun show(host: AnnouncementHost) {
        newAsset?.let {
            host.showAnnouncementCard(
                card = StandardAnnouncementCard(
                    name = name,
                    dismissRule = DismissRule.CardOneTime,
                    dismissEntry = dismissEntry,
                    titleText = R.string.new_asset_card_title,
                    titleFormatParams = arrayOf(it.name, it.ticker),
                    bodyText = R.string.new_asset_card_body,
                    bodyFormatParams = arrayOf(it.ticker),
                    ctaText = R.string.new_asset_card_cta,
                    ctaFormatParams = arrayOf(it.ticker),
                    iconUrl = it.logo,
                    dismissFunction = {
                        host.dismissAnnouncementCard()
                    },
                    ctaFunction = {
                        host.dismissAnnouncementCard()
                        host.startSimpleBuy(it)
                    }
                )
            )
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "NEW_ASSET_ANNOUNCEMENT_DISMISSED"
    }
}