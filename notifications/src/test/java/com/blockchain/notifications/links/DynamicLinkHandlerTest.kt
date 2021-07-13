package com.blockchain.notifications.links

import android.content.Intent
import android.net.Uri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify

import com.nhaarman.mockitokotlin2.any
import org.junit.Test

class DynamicLinkHandlerTest {

    private val testOnSuccessListener = argumentCaptor<OnSuccessListener<PendingDynamicLinkData>>()
    private val testOnFailureListener = argumentCaptor<OnFailureListener>()

    @Test
    fun `returns uri if present`() {
        val uri: Uri = mock()
        val data = mock<PendingDynamicLinkData> {
            on { link }.thenReturn(uri)
        }
        val task = mock<Task<PendingDynamicLinkData>> {
            on { addOnSuccessListener(testOnSuccessListener.capture()) }.thenReturn(it)
            on { addOnFailureListener(testOnFailureListener.capture()) }.thenReturn(it)
        }
        val dynamicLinks = mock<FirebaseDynamicLinks> {
            on { getDynamicLink(any<Intent>()) }.thenReturn(task)
        }

        val testObserver = DynamicLinkHandler(dynamicLinks)
            .getPendingLinks(mock())
            .test()

        testOnSuccessListener.firstValue.onSuccess(data)

        testObserver
            .assertNoErrors()
            .assertComplete()
            .assertValue(uri)
    }

    @Test
    fun `completes if uri not present`() {
        val data = mock<PendingDynamicLinkData> {
            on { link }.thenReturn(null)
        }
        val task = mock<Task<PendingDynamicLinkData>> {
            on { addOnSuccessListener(testOnSuccessListener.capture()) }.thenReturn(it)
            on { addOnFailureListener(testOnFailureListener.capture()) }.thenReturn(it)
        }
        val dynamicLinks = mock<FirebaseDynamicLinks> {
            on { getDynamicLink(any<Intent>()) }.thenReturn(task)
        }

        val testObserver = DynamicLinkHandler(dynamicLinks)
            .getPendingLinks(mock())
            .test()

        testOnSuccessListener.firstValue.onSuccess(data)

        verify(dynamicLinks).getDynamicLink(any<Intent>())

        testObserver
            .assertNoErrors()
            .assertComplete()
            .assertNoValues()
    }

    @Test
    fun `returns failure present`() {
        val task = mock<Task<PendingDynamicLinkData>> {
            on { addOnSuccessListener(testOnSuccessListener.capture()) }.thenReturn(it)
            on { addOnFailureListener(testOnFailureListener.capture()) }.thenReturn(it)
        }
        val dynamicLinks = mock<FirebaseDynamicLinks> {
            on { getDynamicLink(any<Intent>()) }.thenReturn(task)
        }

        val testObserver = DynamicLinkHandler(dynamicLinks)
            .getPendingLinks(mock())
            .test()

        testOnFailureListener.firstValue.onFailure(Exception())

        verify(dynamicLinks).getDynamicLink(any<Intent>())

        testObserver
            .assertError(Exception::class.java)
    }
}