package com.blockchain.logging

import com.blockchain.android.testutils.rxInit
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.reactivex.rxjava3.core.Observable

import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.SettingsService
import java.util.Calendar

class LastTxUpdateDateOnSettingsServiceTest {

    @get:Rule
    val rxSchedulers = rxInit {
        ioTrampoline()
    }

    @Test
    fun `updates time successfully with time set to midnight`() {
        val captor = argumentCaptor<String>()
        val mockSettings = mock<SettingsService> {
            on { updateLastTxTime(captor.capture()) }.thenReturn(Observable.just(mock()))
        }

        LastTxUpdateDateOnSettingsService(mockSettings).updateLastTxTime()
            .test()
            .assertComplete()

        verify(mockSettings).updateLastTxTime(captor.capture())

        val time = Calendar.getInstance().apply { timeInMillis = captor.firstValue.toLong() }
        with(time) {
            get(Calendar.HOUR_OF_DAY) `should be equal to` 0
            get(Calendar.MINUTE) `should be equal to` 0
            get(Calendar.SECOND) `should be equal to` 0
            get(Calendar.MILLISECOND) `should be equal to` 0
        }
    }

    @Test
    fun `call fails but still triggers complete`() {
        val mockSettings = mock<SettingsService> {
            on { updateLastTxTime(any()) }.thenReturn(Observable.error { Throwable() })
        }

        LastTxUpdateDateOnSettingsService(mockSettings).updateLastTxTime()
            .test()
            .assertComplete()
    }
}