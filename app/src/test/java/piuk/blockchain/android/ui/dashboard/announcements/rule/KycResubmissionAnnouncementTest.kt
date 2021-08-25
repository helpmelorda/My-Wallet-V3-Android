package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.nabu.UserIdentity
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class KycResubmissionAnnouncementTest {
    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val userIdentity: UserIdentity = mock()

    private lateinit var subject: KycResubmissionAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[KycResubmissionAnnouncement.DISMISS_KEY])
            .thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey)
            .thenReturn(KycResubmissionAnnouncement.DISMISS_KEY)

        subject = KycResubmissionAnnouncement(
            userIdentity = userIdentity,
            dismissRecorder = dismissRecorder
        )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, if kyc resubmission is not required`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.isKycResubmissionRequired()).thenReturn(Single.just(false))

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, if kyc resubmission is required`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(userIdentity.isKycResubmissionRequired()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}