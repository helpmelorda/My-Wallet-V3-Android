package piuk.blockchain.android.ui.kyc.reentry

import com.blockchain.nabu.models.responses.nabu.NabuUser

class TiersReentryDecision : ReentryDecision {

    private lateinit var nabuUser: NabuUser
    private val isTierZero: Boolean by lazy {
        nabuUser.tiers?.current == 0
    }

    override fun findReentryPoint(user: NabuUser): ReentryPoint {
        nabuUser = user
        return when {
            tier0UnverifiedEmail() -> ReentryPoint.EmailEntry
            tier0UnselectedCountry() -> ReentryPoint.CountrySelection
            tier0ProfileIncompleteOrResubmitAllowed() &&
                !tier0UnselectedCountry() -> ReentryPoint.Profile
            tier0AndCanAdvance() -> ReentryPoint.Address
            !hasMobileVerified() -> ReentryPoint.MobileEntry
            else -> ReentryPoint.Veriff
        }
    }

    private fun tier0UnverifiedEmail(): Boolean = isTierZero && !nabuUser.emailVerified

    private fun tier0UnselectedCountry(): Boolean = isTierZero && nabuUser.address?.countryCode.isNullOrBlank()

    private fun tier0ProfileIncompleteOrResubmitAllowed(): Boolean {
        return isTierZero &&
            (nabuUser.isProfileIncomplete() ||
                nabuUser.isMarkedForRecoveryResubmission)
    }

    private fun tier0AndCanAdvance() = isTierZero && nabuUser.tiers!!.next == 1

    private fun hasMobileVerified() = nabuUser.mobileVerified
}

private fun NabuUser.isProfileIncomplete() =
    firstName.isNullOrBlank() ||
        lastName.isNullOrBlank() ||
        dob.isNullOrBlank()
