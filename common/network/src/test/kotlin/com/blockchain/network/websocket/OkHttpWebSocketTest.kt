package com.blockchain.network.websocket

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.junit.Test

class OkHttpWebSocketTest {

    private val options = Options(url = "https://blockchain.info/service")
    private val client: OkHttpClient = mock()
    private val socket: WebSocket = mock()

    private val subject = OkHttpWebSocket(client, options, null)

    @Test
    fun `can send one message`() {
        val message = "message"

        whenever(client.newWebSocket(any(), any())).thenReturn(socket)

        subject.apply {
            open()
            send(message)
        }

        verify(socket).send(message)
        verifyNoMoreInteractions(socket)
    }

    @Test
    fun `can close`() {

        whenever(client.newWebSocket(any(), any())).thenReturn(socket)

        subject.apply {
            open()
            close()
        }

        verify(socket).close(1000, "Unnamed WebSocket deliberately stopped")
        verifyNoMoreInteractions(socket)
    }

    @Test
    fun `can send two messages`() {
        val messageA = "messageA"
        val messageB = "messageB"

        whenever(client.newWebSocket(any(), any())).thenReturn(socket)

        subject.apply {
            open()
            send(messageA)
            send(messageB)
        }

        verify(socket).send(messageA)
        verify(socket).send(messageB)
        verifyNoMoreInteractions(socket)
    }
}
