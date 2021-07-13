package com.blockchain.network.websocket

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.reactivex.rxjava3.disposables.Disposable
import org.amshove.kluent.`should be`
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

class WebSocketOpenAsDisposableTests {

    private val webSocket = mock<WebSocket<Any, Any>>()

    @Test
    fun `open disposing calls open`() {
        webSocket.openAsDisposable()
        verify(webSocket).open()
        verifyNoMoreInteractions(webSocket)
    }

    @Test
    fun `dispose isDisposed`() {
        val disposable: Disposable = webSocket.openAsDisposable()
        disposable.isDisposed `should be` false
        disposable.dispose()
        disposable.isDisposed `should be` true
    }

    @Test
    fun `dispose closes`() {
        val disposable: Disposable = webSocket.openAsDisposable()
        disposable.dispose()
        verify(webSocket).open()
        verify(webSocket).close()
        verifyNoMoreInteractions(webSocket)
    }

    @Test
    fun `double dispose closes once`() {
        val disposable: Disposable = webSocket.openAsDisposable()
        disposable.dispose()
        disposable.dispose()
        verify(webSocket).open()
        verify(webSocket).close()
        verifyNoMoreInteractions(webSocket)
    }
}
