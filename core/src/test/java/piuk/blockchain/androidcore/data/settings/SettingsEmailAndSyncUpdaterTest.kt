package piuk.blockchain.androidcore.data.settings

import com.blockchain.nabu.NabuUserSync
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import info.blockchain.wallet.api.data.Settings
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import org.junit.Test

class SettingsEmailAndSyncUpdaterTest {

    @Test
    fun `can get unverified email from settings`() {
        val settings: Settings = mock {
            on { email }.thenReturn("email@blockchain.com")
            on { isEmailVerified }.thenReturn(false)
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.just(settings))
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .email()
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` "email@blockchain.com"
                isVerified `should be` false
            }
    }

    @Test
    fun `can get verified email from settings`() {
        val settings: Settings = mock {
            on { email }.thenReturn("otheremail@emaildomain.com")
            on { isEmailVerified }.thenReturn(true)
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.just(settings))
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .email()
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` "otheremail@emaildomain.com"
                isVerified `should be` true
            }
    }

    @Test
    fun `missing settings returns empty email`() {
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.empty())
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .email()
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` ""
                isVerified `should be` false
            }
    }

    @Test
    fun `can update email in settings`() {
        val oldSettings: Settings = mock {
            on { email }.thenReturn("oldemail@blockchain.com")
            on { isEmailVerified }.thenReturn(false)
        }
        val settings: Settings = mock {
            on { email }.thenReturn("newemail@blockchain.com")
            on { isEmailVerified }.thenReturn(false)
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.just(oldSettings))
            on { updateEmail("newemail@blockchain.com", null) }.thenReturn(Observable.just(settings))
        }
        val nabuUserSync = expectToSync()
        SettingsEmailAndSyncUpdater(settingsDataManager, nabuUserSync)
            .updateEmailAndSync("newemail@blockchain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` "newemail@blockchain.com"
                isVerified `should be` false
            }
        verify(settingsDataManager).fetchSettings()
        verify(settingsDataManager).updateEmail("newemail@blockchain.com", null)
        verifyNoMoreInteractions(settingsDataManager)
        verify(nabuUserSync).syncUser()
    }

    @Test
    fun `can resend email in settings`() {
        val settings: Settings = mock {
            on { email }.thenReturn("oldemail@blockchain.com")
            on { isEmailVerified }.thenReturn(false)
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { updateEmail(any()) }.thenReturn(Observable.just(settings))
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .resendEmail("oldemail@blockchain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` "oldemail@blockchain.com"
                isVerified `should be` false
            }
        verify(settingsDataManager).updateEmail("oldemail@blockchain.com")
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `if the email is verified, when you try to change it to the same thing, it does not update`() {
        val settings: Settings = mock {
            on { email }.thenReturn("theemail@emaildomain.com")
            on { isEmailVerified }.thenReturn(true)
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.just(settings))
        }
        SettingsEmailAndSyncUpdater(settingsDataManager, notExpectingSync())
            .updateEmailAndSync("theemail@emaildomain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` "theemail@emaildomain.com"
                isVerified `should be` true
            }
        verify(settingsDataManager).fetchSettings()
        verify(settingsDataManager, never()).updateEmail(any())
        verifyNoMoreInteractions(settingsDataManager)
    }

    @Test
    fun `if the email is not-verified, when you try to change it to the same thing, it does update`() {
        val settings: Settings = mock {
            on { email }.thenReturn("theemail@emaildomain.com")
            on { isEmailVerified }.thenReturn(false)
        }
        val settingsDataManager: SettingsDataManager = mock {
            on { fetchSettings() }.thenReturn(Observable.just(settings))
            on { updateEmail("theemail@emaildomain.com", null) }.thenReturn(Observable.just(settings))
        }
        val nabuUserSync = expectToSync()
        SettingsEmailAndSyncUpdater(settingsDataManager, nabuUserSync)
            .updateEmailAndSync("theemail@emaildomain.com")
            .test()
            .assertComplete()
            .values()
            .single().apply {
                address `should be equal to` "theemail@emaildomain.com"
                isVerified `should be` false
            }
        verify(settingsDataManager).fetchSettings()
        verify(settingsDataManager).updateEmail("theemail@emaildomain.com", null)
        verifyNoMoreInteractions(settingsDataManager)
        verify(nabuUserSync).syncUser()
    }
}

private fun expectToSync(): NabuUserSync =
    mock {
        on { syncUser() }.thenReturn(Completable.complete())
    }

private fun notExpectingSync(): NabuUserSync =
    mock {
        on { syncUser() }.doThrow(RuntimeException("Not expecting to sync the user"))
    }
