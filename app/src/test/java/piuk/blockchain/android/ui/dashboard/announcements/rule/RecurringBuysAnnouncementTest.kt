package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class RecurringBuysAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val announcementQueries: AnnouncementQueries = mock()
    private val currencyPrefs: CurrencyPrefs = mock()

    private lateinit var subject: RecurringBuysAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[RecurringBuysAnnouncement.DISMISS_KEY])
            .thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey)
            .thenReturn(RecurringBuysAnnouncement.DISMISS_KEY)

        subject =
            RecurringBuysAnnouncement(
                dismissRecorder = dismissRecorder,
                announcementQueries = announcementQueries,
                currencyPrefs = currencyPrefs
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
    fun `should show, when not already shown, and has fiat funds`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(announcementQueries.hasFundedFiatWallets()).thenReturn(Single.just(true))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("EUR")

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, and has USD fiat selected`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(announcementQueries.hasFundedFiatWallets()).thenReturn(Single.just(false))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("USD")

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, no fiat funds and USD not selected`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(announcementQueries.hasFundedFiatWallets()).thenReturn(Single.just(false))
        whenever(currencyPrefs.selectedFiatCurrency).thenReturn("EUR")

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
