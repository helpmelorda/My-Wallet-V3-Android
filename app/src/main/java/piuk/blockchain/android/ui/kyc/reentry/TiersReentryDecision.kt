package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.ResubmissionResponse

class TiersReentryDecision : ReentryDecision {

    override fun findReentryPoint(user: NabuUser): ReentryPoint {
        val allowResubmit = user.resubmission?.reason == ResubmissionResponse.ACCOUNT_RECOVERED_REASON
        val isTierZero = user.tiers?.current == 0

        return when {
            isTierZero && !user.emailVerified -> ReentryPoint.EmailEntry
            isTierZero && user.address?.countryCode.isNullOrBlank() -> ReentryPoint.CountrySelection
            isTierZero && (user.isProfileIncomplete() || allowResubmit) -> return ReentryPoint.Profile
            isTierZero && user.tiers!!.next == 1 -> ReentryPoint.Address
            !user.mobileVerified -> ReentryPoint.MobileEntry
            else -> ReentryPoint.Veriff
        }
    }
}

private fun NabuUser.isProfileIncomplete() =
    firstName.isNullOrBlank() ||
        lastName.isNullOrBlank() ||
        dob.isNullOrBlank()
