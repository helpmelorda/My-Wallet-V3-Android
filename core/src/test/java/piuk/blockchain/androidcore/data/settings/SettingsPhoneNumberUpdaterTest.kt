package piuk.blockchain.androidcore.data.settings

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Observable

import org.amshove.kluent.`should be equal to`
import com.nhaarman.mockitokotlin2.any
import org.junit.Test

class SettingsPhoneNumberUpdaterTest {

    @Test
    fun `can get number from settings`() {
        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+123456")
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.just(settings))
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .smsNumber()
            .test()
            .assertComplete()
            .values()
            .single() `should be equal to` "+123456"
    }

    @Test
    fun `missing settings returns empty number`() {
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.empty())
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .smsNumber()
            .test()
            .assertComplete()
            .values()
            .single() `should be equal to` ""
    }

    @Test
    fun `can update number in settings with sanitised input`() {
        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+123456")
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { updateSms(any()) }.thenReturn(Observable.just(settings))
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .updateSms(PhoneNumber("+(123)-456-789"))
            .test()
            .assertComplete()
            .values()
            .single() `should be equal to` "+123456"
        verify(settingsDataManager).updateSms("+123456789")
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `can verify code in settings`() {
        val settings: Settings = mock {
            on { smsNumber }.thenReturn("+123456")
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { verifySms(any()) }.thenReturn(Observable.just(settings))
        }
        SettingsPhoneNumberUpdater(settingsDataManager)
            .verifySms("ABC345")
            .test()
            .assertComplete()
            .values()
            .single() `should be equal to` "+123456"
        verify(settingsDataManager).verifySms("ABC345")
        verifyNoMoreInteractions(settingsDataManager)
    }
}
