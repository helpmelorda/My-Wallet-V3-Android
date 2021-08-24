package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.preferences.CurrencyPrefs
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class RecurringBuysAnnouncement(
    dismissRecorder: DismissRecorder,
    val announcementQueries: AnnouncementQueries,
    val currencyPrefs: CurrencyPrefs
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return announcementQueries.hasFundedFiatWallets().map {
            it || currencyPrefs.selectedFiatCurrency == "USD"
        }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardOneTime,
                dismissEntry = dismissEntry,
                titleText = R.string.rb_announcement_title,
                bodyText = R.string.rb_announcement_description,
                ctaText = R.string.rb_announcement_action,
                iconImage = R.drawable.ic_tx_recurring_buy,
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startRecurringBuyUpsell()
                },
                dismissFunction = {
                    host.dismissAnnouncementCard()
                }
            )
        )
    }

    override val name = "rb_available"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "RecurringBuysAnnouncement_DISMISSED"
    }
}