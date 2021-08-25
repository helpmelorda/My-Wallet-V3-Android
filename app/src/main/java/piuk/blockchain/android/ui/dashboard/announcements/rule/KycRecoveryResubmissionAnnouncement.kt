package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import com.blockchain.nabu.UserIdentity
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class KycRecoveryResubmissionAnnouncement(
    dismissRecorder: DismissRecorder,
    private val userIdentity: UserIdentity,
    private val accountRecoveryFF: FeatureFlag
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> =
        accountRecoveryFF.enabled.zipWith(
            userIdentity.shouldResubmitAfterRecovery()
        )
            .map { (enabled, shouldResubmit) ->
                enabled && shouldResubmit
            }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPersistent,
                dismissEntry = dismissEntry,
                titleText = R.string.re_verify_identity_card_title,
                bodyText = R.string.re_verify_identity_card_body,
                ctaText = R.string.re_verify_identity_card_cta,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startKyc(CampaignType.None)
                }
            )
        )
    }

    override val name = "kyc_recovery_resubmission"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KycRecoveryResubmissionAnnouncement_DISMISSED"
    }
}